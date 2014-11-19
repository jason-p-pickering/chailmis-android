/*
 * Copyright (c) 2014, Thoughtworks Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */

package org.clintonhealthaccess.lmis.app.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.TimingLogger;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.j256.ormlite.dao.Dao;
import com.thoughtworks.dhis.models.DataElementType;

import org.clintonhealthaccess.lmis.app.R;
import org.clintonhealthaccess.lmis.app.models.Adjustment;
import org.clintonhealthaccess.lmis.app.models.AdjustmentReason;
import org.clintonhealthaccess.lmis.app.models.Category;
import org.clintonhealthaccess.lmis.app.models.Commodity;
import org.clintonhealthaccess.lmis.app.models.CommodityAction;
import org.clintonhealthaccess.lmis.app.models.DataSet;
import org.clintonhealthaccess.lmis.app.models.StockItem;
import org.clintonhealthaccess.lmis.app.models.StockItemSnapshot;
import org.clintonhealthaccess.lmis.app.models.User;
import org.clintonhealthaccess.lmis.app.models.reports.UtilizationItem;
import org.clintonhealthaccess.lmis.app.models.reports.UtilizationItemName;
import org.clintonhealthaccess.lmis.app.models.reports.UtilizationValue;
import org.clintonhealthaccess.lmis.app.persistence.DbUtil;
import org.clintonhealthaccess.lmis.app.remote.LmisServer;
import org.clintonhealthaccess.lmis.app.utils.DateUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import roboguice.inject.InjectResource;

import static org.clintonhealthaccess.lmis.app.persistence.DbUtil.Operation;

public class CommodityService {
    public static final String MONTHLY_STOCK_COUNT_DAY = "MONTHLY_STOCK_COUNT_DAY";
    public static final String ROUTINE_ORDER_ALERT_DAY = "ROUTINE_ORDER_ALERT_DAY";
    @Inject
    private LmisServer lmisServer;

    @Inject
    private CategoryService categoryService;

    @Inject
    private AllocationService allocationService;

    @Inject
    private AdjustmentService adjustmentService;

    @Inject
    CommodityActionService commodityActionService;

    @Inject
    StockItemSnapshotService stockItemSnapshotService;

    @Inject
    ReceiveService receiveService;

    @Inject
    private DbUtil dbUtil;

    @Inject
    private Context context;

    @Inject
    SharedPreferences sharedPreferences;

    @InjectResource(R.string.monthly_stock_count_search_key)
    private String monthlyStockCountSearchKey;

    @InjectResource(R.string.routine_order_alert_day)
    private String routineOrderAlertDay;
    @Inject
    private DispensingService dispensingService;

    public void initialise(User user) {
        TimingLogger timingLogger = new TimingLogger("TIMER", "initialise");
        List<Category> allCommodities = lmisServer.fetchCommodities(user);
        timingLogger.addSplit("fetch all cats");
        saveToDatabase(allCommodities);
        timingLogger.addSplit("save all Cats");
        categoryService.clearCache();
        syncConstants(user);
        timingLogger.addSplit("sync constants");

        List<Commodity> commodities = all();
        timingLogger.addSplit("all");

        Log.i("Inital sync:", "<========== syncing Commodity Action Values");
        commodityActionService.syncCommodityActionValues(user, commodities);

        timingLogger.addSplit("actionValues");
        categoryService.clearCache();
        updateStockValues(all());
        categoryService.clearCache();
        createInitialStockItemSnapShots(all());
        timingLogger.addSplit("updateStockValues");
        allocationService.syncAllocations(user);
        timingLogger.addSplit("sync allocations");
        categoryService.clearCache();
        timingLogger.addSplit("clearCache");
        timingLogger.dumpToLog();
    }

    private void createInitialStockItemSnapShots(final List<Commodity> commodities) {
        dbUtil.withDaoAsBatch(StockItemSnapshot.class, new Operation<StockItemSnapshot, Void>() {
            @Override
            public Void operate(Dao<StockItemSnapshot, String> dao) throws SQLException {
                    Log.i("Bin Card:", "initial snapshots");
                for(Commodity commodity : commodities){
                    StockItemSnapshot stockItemSnapshot = new StockItemSnapshot(commodity, new Date(), commodity.getStockOnHand());
                    Log.i("Bin Card:", "snapshot; "+stockItemSnapshot);

                    dao.createOrUpdate(stockItemSnapshot);
                }
                return null;
            }
        });
    }

    private void syncConstants(User user) {
        fetchAndSaveIntegerConstant(user, monthlyStockCountSearchKey, MONTHLY_STOCK_COUNT_DAY);
        fetchAndSaveIntegerConstant(user, routineOrderAlertDay, ROUTINE_ORDER_ALERT_DAY);
    }

    private void updateStockValues(List<Commodity> commodities) {
        List<StockItem> stockItems = FluentIterable.from(commodities).transform(new Function<Commodity, StockItem>() {
            @Override
            public StockItem apply(Commodity input) {
                CommodityAction commodityAction = input.getCommodityAction(DataElementType.STOCK_ON_HAND.getActivity());
                if (commodityAction != null && commodityAction.getActionLatestValue() != null) {
                    return new StockItem(input, Integer.parseInt(commodityAction.getActionLatestValue().getValue()));
                } else {
                    return new StockItem(input, 0);
                }
            }
        }).toList();

        for (StockItem item : stockItems) {
            createStock(item);
        }
    }

    private void fetchAndSaveIntegerConstant(User user, String stockCountSearchKey, String key) {
        Integer day = lmisServer.fetchIntegerConstant(user, stockCountSearchKey);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, day);
        editor.commit();
    }

    private void createStock(final StockItem item) {
        dbUtil.withDao(StockItem.class, new Operation<StockItem, Void>() {
            @Override
            public Void operate(Dao<StockItem, String> dao) throws SQLException {
                dao.create(item);
                return null;
            }
        });
    }


    public List<Commodity> all() {
        List<Category> categories = categoryService.all();
        List<Commodity> commodities = new ArrayList<>();
        for (Category category : categories) {
            commodities.addAll(category.getCommodities());
        }
        return commodities;
    }

    public void saveToDatabase(final List<Category> allCommodities) {
        dbUtil.withDaoAsBatch(Category.class, new Operation<Category, Void>() {
            @Override
            public Void operate(Dao<Category, String> dao) throws SQLException {
                for (Category category : allCommodities) {
                    dao.createOrUpdate(category);
                    saveAllCommodities(category);
                }
                return null;
            }
        });
    }

    private void saveAllCommodities(final Category category) {
        dbUtil.withDaoAsBatch(Commodity.class, new Operation<Commodity, Void>() {
            @Override
            public Void operate(Dao<Commodity, String> dao) throws SQLException {
                for (Commodity commodity : category.getNotSavedCommodities()) {
                    commodity.setCategory(category);
                    dao.createOrUpdate(commodity);
                    createCommodityAction(commodity);
                }
                return null;
            }
        });
    }

    private void createCommodityAction(Commodity commodity) {
        GenericDao<CommodityAction> commodityActivityGenericDao = new GenericDao<>(CommodityAction.class, context);
        GenericDao<DataSet> dataSetGenericDao = new GenericDao<>(DataSet.class, context);
        final List<DataSet> dataSets = new ArrayList<>();
        final List<CommodityAction> actions = new ArrayList<>();
        for (CommodityAction commodityAction : commodity.getCommodityActions()) {
            if (commodityAction.getDataSet() != null) {
                dataSets.add(commodityAction.getDataSet());
            }
            if (commodityAction.getCommodity() == null) {
                commodityAction.setCommodity(commodity);
            }
            actions.add(commodityAction);
        }

        dataSetGenericDao.bulkOperation(new Operation<DataSet, Object>() {
            @Override
            public Object operate(Dao<DataSet, String> dao) throws SQLException {
                for (DataSet dataSet : dataSets) {
                    dao.createOrUpdate(dataSet);
                }
                return null;
            }
        });

        commodityActivityGenericDao.bulkOperation(new Operation<CommodityAction, Object>() {
            @Override
            public Object operate(Dao<CommodityAction, String> dao) throws SQLException {
                for (CommodityAction action : actions) {
                    dao.createOrUpdate(action);
                }
                return null;
            }
        });

    }

    public List<Commodity> getMost5HighlyConsumedCommodities() {

        List<Commodity> commodities = all();
        Collections.sort(commodities, new Comparator<Commodity>() {
            @Override
            public int compare(Commodity lhs, Commodity rhs) {
                return rhs.getAMC().compareTo(lhs.getAMC());
            }
        });
        if (commodities.size() > 5) {
            return commodities.subList(0, 5);
        } else {
            return commodities;
        }
    }

    public List<UtilizationItem> getMonthlyUtilizationItems(Commodity commodity, Date date) throws Exception {
        Date monthEndDate = DateUtil.getMonthEndDate(date);
        Date monthStartDate = DateUtil.getMonthStartDate(date);

        List<UtilizationItem> utilizationItems = new ArrayList<>();
        for (UtilizationItemName utilizationItemName : UtilizationItemName.values()) {

            if (utilizationItemName.equals(UtilizationItemName.DAY_OF_MONTH)) {
                utilizationItems.add(new UtilizationItem(utilizationItemName.getName(),
                        getDaysOfMonth(commodity, monthStartDate, monthEndDate)));
            }

            if (utilizationItemName.equals(UtilizationItemName.OPENING_BALANCE)) {
                utilizationItems.add(new UtilizationItem(utilizationItemName.getName(),
                        getOpeningBalances(commodity, monthStartDate, monthEndDate)));
            }

            if (utilizationItemName.equals(UtilizationItemName.RECEIVED)) {
                utilizationItems.add(new UtilizationItem(utilizationItemName.getName(),
                        receiveService.getReceivedValues(commodity, monthStartDate, monthEndDate)));
            }

            if (utilizationItemName.equals(UtilizationItemName.DOSES_OPENED) && !commodity.isDevice()
                    || utilizationItemName.equals(UtilizationItemName.USED) && commodity.isDevice()) {
                utilizationItems.add(new UtilizationItem(utilizationItemName.getName(),
                        dispensingService.getDispensedValues(commodity, monthStartDate, monthEndDate, utilizationItemName.equals(UtilizationItemName.DOSES_OPENED))));
            }

            if (utilizationItemName.equals(UtilizationItemName.ENDING_BALANCE)) {
                utilizationItems.add(new UtilizationItem(utilizationItemName.getName(),
                        getEndingBalance(commodity, monthStartDate, monthEndDate)));
            }

            if (utilizationItemName.equals(UtilizationItemName.QUANTITY_RETURNED_TO_LGA)) {
                utilizationItems.add(new UtilizationItem(utilizationItemName.getName(),
                        getReturnedToLGA(commodity, monthStartDate, monthEndDate)));
            }
        }

        return utilizationItems;
    }

    private List<UtilizationValue> getDaysOfMonth(Commodity commodity, Date startDate, Date endDate) {
        Calendar calendar = DateUtil.calendarDate(startDate);
        List<UtilizationValue> utilizationValues = new ArrayList<>();

        Date upperLimitDate = DateUtil.addDayOfMonth(endDate, 1);
        while (calendar.getTime().before(upperLimitDate)) {
            UtilizationValue utilizationValue = new UtilizationValue(DateUtil.dayNumber(calendar.getTime()),
                    DateUtil.dayNumber(calendar.getTime()));
            utilizationValues.add(utilizationValue);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return utilizationValues;
    }

    private List<UtilizationValue> getReturnedToLGA(Commodity commodity, Date startDate, Date endDate) {
        List<Adjustment> adjustments = adjustmentService.getAdjustments(commodity,
                startDate, endDate, AdjustmentReason.RETURNED_TO_LGA);

        List<UtilizationValue> utilizationValues = new ArrayList<>();

        Calendar calendar = DateUtil.calendarDate(startDate);

        Date upperLimitDate = DateUtil.addDayOfMonth(endDate, 1);

        while (calendar.getTime().before(upperLimitDate)) {

            int totalAdjustments = getTotalAdjustments(calendar.getTime(), adjustments);
            UtilizationValue utilizationValue = new UtilizationValue(DateUtil.dayNumber(calendar.getTime()), totalAdjustments);
            utilizationValues.add(utilizationValue);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return utilizationValues;
    }

    private int getTotalAdjustments(Date date, List<Adjustment> adjustments) {
        int totalAdjustments = 0;
        for (Adjustment adjustment : adjustments) {
            if (DateUtil.equal(adjustment.getCreated(), date)){
                totalAdjustments += adjustment.getQuantity();
            }
        }
        return totalAdjustments;
    }

    private List<UtilizationValue> getEndingBalance(Commodity commodity, Date startDate, Date endDate) throws Exception {
        List<StockItemSnapshot> stockItemSnapshots = stockItemSnapshotService.get(commodity,
                DateUtil.addDayOfMonth(startDate, -1), endDate);

        List<UtilizationValue> utilizationValues = new ArrayList<>();

        int previousDaysClosingStock = stockItemSnapshotService.getLatestStock(commodity, startDate, false);

        Calendar calendar = DateUtil.calendarDate(startDate);

        Date upperLimitDate = DateUtil.addDayOfMonth(endDate, 1);
        while (calendar.getTime().before(upperLimitDate)) {

            StockItemSnapshot closingStockSnapshot = stockItemSnapshotService.getSnapshot(
                    calendar.getTime(), stockItemSnapshots);

            int closingBalance = closingStockSnapshot == null ? previousDaysClosingStock :
                    closingStockSnapshot.getQuantity();

            UtilizationValue utilizationValue = new UtilizationValue(DateUtil.dayNumber(calendar.getTime()), closingBalance);
            utilizationValues.add(utilizationValue);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            previousDaysClosingStock = closingBalance;
        }

        return utilizationValues;
    }

    private List<UtilizationValue> getOpeningBalances(Commodity commodity, Date startDate, Date endDate) throws Exception {
        List<StockItemSnapshot> stockItemSnapshots = stockItemSnapshotService.get(commodity,
                DateUtil.addDayOfMonth(startDate, -1), endDate);

        List<UtilizationValue> utilizationValues = new ArrayList<>();

        int openingStock = stockItemSnapshotService.getLatestStock(commodity, startDate, true);
        int previousDaysOpeningStock = openingStock;

        Calendar calendar = DateUtil.calendarDate(startDate);

        Date upperLimitDate = DateUtil.addDayOfMonth(endDate, 1);
        while (calendar.getTime().before(upperLimitDate)) {

            Date previousDay = DateUtil.addDayOfMonth(calendar.getTime(), -1);
            StockItemSnapshot openingStockSnapshot = stockItemSnapshotService.getSnapshot(
                    previousDay, stockItemSnapshots);

            int openingBalance = openingStockSnapshot == null ? previousDaysOpeningStock :
                    openingStockSnapshot.getQuantity();
            utilizationValues.add(new UtilizationValue(DateUtil.dayNumber(calendar.getTime()), openingBalance));
            previousDaysOpeningStock = openingBalance;
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return utilizationValues;
    }
}
