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

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.j256.ormlite.dao.Dao;

import org.clintonhealthaccess.lmis.app.models.AdjustmentReason;
import org.clintonhealthaccess.lmis.app.models.Category;
import org.clintonhealthaccess.lmis.app.models.Commodity;
import org.clintonhealthaccess.lmis.app.models.CommodityActionValue;
import org.clintonhealthaccess.lmis.app.models.Dispensing;
import org.clintonhealthaccess.lmis.app.models.DispensingItem;
import org.clintonhealthaccess.lmis.app.models.User;
import org.clintonhealthaccess.lmis.app.models.reports.ConsumptionValue;
import org.clintonhealthaccess.lmis.app.models.reports.FacilityCommodityConsumptionRH1ReportItem;
import org.clintonhealthaccess.lmis.app.models.reports.FacilityConsumptionReportRH2Item;
import org.clintonhealthaccess.lmis.app.models.reports.FacilityStockReportItem;
import org.clintonhealthaccess.lmis.app.models.reports.MonthlyVaccineUtilizationReportItem;
import org.clintonhealthaccess.lmis.app.persistence.DbUtil;
import org.clintonhealthaccess.lmis.app.remote.LmisServer;
import org.clintonhealthaccess.lmis.utils.RobolectricGradleTestRunner;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.clintonhealthaccess.lmis.utils.LMISTestCase.adjust;
import static org.clintonhealthaccess.lmis.utils.LMISTestCase.createStockItemSnapshot;
import static org.clintonhealthaccess.lmis.utils.LMISTestCase.createStockItemSnapshotValue;
import static org.clintonhealthaccess.lmis.utils.LMISTestCase.dispense;
import static org.clintonhealthaccess.lmis.utils.LMISTestCase.lose;
import static org.clintonhealthaccess.lmis.utils.LMISTestCase.receive;
import static org.clintonhealthaccess.lmis.utils.TestFixture.defaultCategories;
import static org.clintonhealthaccess.lmis.utils.TestInjectionUtil.setUpInjection;
import static org.clintonhealthaccess.lmis.utils.TestInjectionUtil.testActionValues;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.application;

@RunWith(RobolectricGradleTestRunner.class)
public class ReportsServiceTest {

    @Inject
    CategoryService categoryService;

    @Inject
    private ReportsService reportsService;

    private List<CommodityActionValue> mockStockLevels;

    private LmisServer mockLmisServer;

    @Inject
    private CommodityService commodityService;

    private List<Category> categories;

    @Inject
    ReceiveService receiveService;

    @Inject
    private DispensingService dispensingService;

    @Inject
    private LossService lossService;

    @Inject
    private AdjustmentService adjustmentService;

    @Inject
    private DbUtil dbUtil;

    private SimpleDateFormat dateFormatYear;

    private SimpleDateFormat dateFormatMonth;

    @Before
    public void setUp() throws Exception {
        mockLmisServer = mock(LmisServer.class);
        mockStockLevels = testActionValues(application);
        when(mockLmisServer.fetchCommodities((User) anyObject())).thenReturn(defaultCategories(application));
        when(mockLmisServer.fetchCommodityActionValues(anyList(), (User) anyObject())).thenReturn(mockStockLevels);

        setUpInjection(this, new AbstractModule() {
            @Override
            protected void configure() {
                bind(LmisServer.class).toInstance(mockLmisServer);
            }
        });

        commodityService.initialise(new User("test", "pass"));
        categories = categoryService.all();

        dateFormatYear = new SimpleDateFormat("YYYY");
        dateFormatMonth = new SimpleDateFormat("MMMM");
    }

    @Test
    public void shouldReturnListOfFacilityStockReportItems() throws Exception {
        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(categories.get(0), "2014", "July", "2014", "July");
        assertThat(facilityStockReportItems.size(), is(greaterThan(0)));
    }

    @Test
    public void shouldReturnCorrectNumberOfFacilityStockReportItems() throws Exception {
        Category category = categories.get(0);
        int numberOfCommodities = category.getCommodities().size();
        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, "2014", "July", "2014", "July");
        assertThat(facilityStockReportItems.size(), is(numberOfCommodities));

        category = categories.get(1);
        numberOfCommodities = category.getCommodities().size();
        facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, "2014", "July", "2014", "July");
        assertThat(facilityStockReportItems.size(), is(numberOfCommodities));
    }

    @Test
    public void shouldReturnCorrectOpeningBalance() throws Exception {

        Category category = categories.get(0);
        Commodity commodity = category.getCommodities().get(0);

        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        int difference = 3;
        createStockItemSnapshot(commodity, calendar.getTime(), difference);

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date startDate = calendar.getTime();

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        int expectedQuantity = commodity.getStockOnHand() + difference;
        int openingStock = facilityStockReportItems.get(0).getOpeningStock();
        assertThat(openingStock, is(expectedQuantity));
    }

    @Test
    public void shouldReturnCorrectQuantityOfCommoditiesReceived() throws Exception {
        Category category = categories.get(0);
        Commodity commodity = category.getCommodities().get(0);

        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date startDate = calendar.getTime();

        receive(commodity, 20, receiveService);
        receive(commodity, 30, receiveService);

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category,
                dateFormatYear.format(startDate), dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        assertThat(facilityStockReportItems.get(0).getCommoditiesReceived(), is(50));
    }

    @Test
    public void shouldReturnCorrectQuantityDispensed() throws Exception {
        Category category = categoryService.all().get(0);
        Commodity commodity = category.getCommodities().get(0);

        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date startDate = calendar.getTime();

        dispense(commodity, 2, dispensingService);
        dispense(commodity, 1, dispensingService);

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category,
                dateFormatYear.format(startDate), dateFormatMonth.format(startDate), dateFormatYear.format(endDate),
                dateFormatMonth.format(endDate));

        assertThat(facilityStockReportItems.get(0).getCommoditiesDispensed(), is(3));
    }

    @Test
    public void shouldReturnValidQuantityLost() throws Exception {
        Category category = categories.get(0);
        Commodity commodity = category.getCommodities().get(0);

        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date startDate = calendar.getTime();

        lose(commodity, 2, lossService);
        lose(commodity, 2, lossService);

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category,
                dateFormatYear.format(startDate), dateFormatMonth.format(startDate), dateFormatYear.format(endDate),
                dateFormatMonth.format(endDate));

        assertThat(facilityStockReportItems.get(0).getCommoditiesLost(), is(4));
    }

    @Test
    public void shouldReturnTotalQuantityAdjusted() throws Exception {
        Category category = categories.get(0);
        Commodity commodity = category.getCommodities().get(0);

        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date startDate = calendar.getTime();

        adjust(commodity, 6, true, AdjustmentReason.RECEIVED_FROM_ANOTHER_FACILITY, adjustmentService);
        adjust(commodity, 13, false, AdjustmentReason.PHYSICAL_COUNT, adjustmentService);

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category,
                dateFormatYear.format(startDate), dateFormatMonth.format(startDate), dateFormatYear.format(endDate),
                dateFormatMonth.format(endDate));

        assertThat(facilityStockReportItems.get(0).getCommoditiesAdjusted(), is(-7));

    }

    @Test
    public void shouldReturnCorrectStockOnHand() throws Exception {
        Category category = categories.get(0);
        Commodity commodity = category.getCommodities().get(0);

        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

        calendar.add(Calendar.DAY_OF_MONTH, -2);
        int difference = 3;
        createStockItemSnapshot(commodity, calendar.getTime(), difference);

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date startDate = calendar.getTime();

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(currentDate), dateFormatMonth.format(currentDate));

        int expectedQuantity = commodity.getStockOnHand() + difference;
        int stockOnHand = facilityStockReportItems.get(0).getStockOnHand();
        assertThat(stockOnHand, is(expectedQuantity));
    }

    @Test
    public void shouldReturnCorrectAMC() throws Exception {
        Category category = categories.get(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.APRIL, 01);
        Date startDate = calendar.getTime();

        calendar.set(2014, Calendar.APRIL, 07);
        Date endDate = calendar.getTime();

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        int expectedAMC = 103;
        assertThat(facilityStockReportItems.get(0).getCommodityAMC(), is(expectedAMC));
    }

    @Test
    public void shouldReturnCorrectAMCFor2Months() throws Exception {
        Category category = categories.get(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.APRIL, 01);
        Date startDate = calendar.getTime();

        calendar.set(2014, Calendar.MAY, 07);
        Date endDate = calendar.getTime();

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        int expectedMaxThreshold = 119;
        assertThat(facilityStockReportItems.get(0).getCommodityAMC(), is(expectedMaxThreshold));
    }

    @Test
    public void shouldReturnMaximumThreshold() throws Exception {
        Category category = categories.get(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.APRIL, 01);
        Date startDate = calendar.getTime();

        calendar.set(2014, Calendar.APRIL, 07);
        Date endDate = calendar.getTime();

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        int expectedMaxThreshold = 40;
        assertThat(facilityStockReportItems.get(0).getCommodityMaxThreshold(), is(expectedMaxThreshold));
    }

    @Test
    public void shouldReturnCorrectMaximumThreshold() throws Exception {
        Category category = categories.get(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.MAY, 01);
        Date startDate = calendar.getTime();

        calendar.set(2014, Calendar.MAY, 07);
        Date endDate = calendar.getTime();

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        int expectedMaxThreshold = 60;
        assertThat(facilityStockReportItems.get(0).getCommodityMaxThreshold(), is(expectedMaxThreshold));
    }

    @Test
    public void shouldReturnCorrectMaxThresholdFor2Months() throws Exception {
        Category category = categories.get(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.APRIL, 01);
        Date startDate = calendar.getTime();

        calendar.set(2014, Calendar.MAY, 07);
        Date endDate = calendar.getTime();

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        int expectedMaxThreshold = 50;
        assertThat(facilityStockReportItems.get(0).getCommodityMaxThreshold(), is(expectedMaxThreshold));
    }

    @Test
    public void shouldReturnNumberOfStockOutDays() throws Exception {
        Category category = categories.get(0);
        Commodity commodity = category.getCommodities().get(0);

        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date startDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        int difference = -5;
        createStockItemSnapshot(commodity, calendar.getTime(), difference);

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        int stockOutDay = 10;
        calendar.add(Calendar.DAY_OF_MONTH, stockOutDay);
        difference = -10;
        createStockItemSnapshot(commodity, calendar.getTime(), difference);

        int numOfStockOutDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - (stockOutDay + 1);

        List<FacilityStockReportItem> facilityStockReportItems =
                reportsService.getFacilityReportItemsForCategory(category,
                        dateFormatYear.format(startDate), dateFormatMonth.format(startDate),
                        dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        assertThat(facilityStockReportItems.get(0).getCommodityStockOutDays(), is(numOfStockOutDays));
    }


    public void shouldReturnMinimumThreshold() throws Exception {
        Category category = categories.get(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.APRIL, 01);
        Date startDate = calendar.getTime();

        calendar.set(2014, Calendar.APRIL, 07);
        Date endDate = calendar.getTime();

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        int expectedMinThreshold = 15;
        assertThat(facilityStockReportItems.get(0).getCommodityMinimumThreshold(), is(expectedMinThreshold));
    }

    @Test
    public void shouldReturnCorrectMinimumThresholdFor2Months() throws Exception {
        Category category = categories.get(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.APRIL, 01);
        Date startDate = calendar.getTime();

        calendar.set(2014, Calendar.MAY, 07);
        Date endDate = calendar.getTime();

        List<FacilityStockReportItem> facilityStockReportItems = reportsService.getFacilityReportItemsForCategory(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        int expectedMinThreshold = 17;
        assertThat(facilityStockReportItems.get(0).getCommodityMinimumThreshold(), is(expectedMinThreshold));
    }

    @Test
    public void shouldReturnCorrectFacilityConsumptionRH2Items() throws Exception {
        Category category = categories.get(0);
        Commodity commodity = category.getCommodities().get(0);

        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        int openingStock = 3;
        createStockItemSnapshotValue(commodity, calendar.getTime(), openingStock);

        receive(commodity, 20, receiveService);
        receive(commodity, 30, receiveService);

        dispense(commodity, 2, dispensingService);
        dispense(commodity, 3, dispensingService);

        lose(commodity, 9, lossService);
        lose(commodity, 3, lossService);

        adjust(commodity, 3, false, AdjustmentReason.SENT_TO_ANOTHER_FACILITY, adjustmentService);
        adjust(commodity, 5, false, AdjustmentReason.SENT_TO_ANOTHER_FACILITY, adjustmentService);

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date startDate = calendar.getTime();

        List<FacilityConsumptionReportRH2Item> facilityConsumptionReportRH2Items = reportsService.getFacilityConsumptionReportRH2Items(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));

        assertThat(facilityConsumptionReportRH2Items.get(0).getOpeningStock(), is(openingStock));
        assertThat(facilityConsumptionReportRH2Items.get(0).getCommoditiesReceived(), is(50));
        assertThat(facilityConsumptionReportRH2Items.get(0).getCommoditiesDispensedToClients(), is(5));

        assertThat(facilityConsumptionReportRH2Items.get(0).getCommoditiesDispensedToFacilities(), is(8));
        assertThat(facilityConsumptionReportRH2Items.get(0).totalDispensed(), is(13));

        assertThat(facilityConsumptionReportRH2Items.get(0).getCommoditiesLost(), is(12));
        assertThat(facilityConsumptionReportRH2Items.get(0).getClosingStock(), is(35));
    }

    @Test
    public void shouldReturnValuesForEachCommodityInTheCategoryForgetFacilityCommodityConsumptionReportRH1() throws Exception {

        Category category = categories.get(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.APRIL, 01);
        Date startDate = calendar.getTime();

        calendar.set(2014, Calendar.MAY, 07);

        Date endDate = calendar.getTime();
        List<FacilityCommodityConsumptionRH1ReportItem> facilityStockReportItems = reportsService.getFacilityCommodityConsumptionReportRH1(category, dateFormatYear.format(startDate),
                dateFormatMonth.format(startDate), dateFormatYear.format(endDate), dateFormatMonth.format(endDate));
        assertThat(facilityStockReportItems.size(), is(6));
    }


    @Test
    public void shouldCalculateConsumptionWithAllDispensingItemsInDateRange() throws Exception {

        Category category = categories.get(0);
        Commodity commodity = category.getCommodities().get(0);


        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.APRIL, 01);
        Date startDate = calendar.getTime();

        calendar.set(2014, Calendar.APRIL, 02);
        createDispensingItemWithDate(commodity, calendar.getTime(), 10);

        calendar.set(2014, Calendar.APRIL, 03);
        createDispensingItemWithDate(commodity, calendar.getTime(), 10);

        calendar.set(2014, Calendar.APRIL, 04);
        createDispensingItemWithDate(commodity, calendar.getTime(), 10);
        createDispensingItemWithDate(commodity, calendar.getTime(), 20);

        calendar.set(2014, Calendar.APRIL, 06);
        Date endDate = calendar.getTime();

        System.out.println("512   ");

        ArrayList<ConsumptionValue> values = reportsService.getConsumptionValuesForCommodityBetweenDates(commodity, startDate, endDate);
        assertThat(values.size(), is(daysBetween(startDate, endDate)));
        assertThat(values.get(0).getConsumption(), is(0));
        assertThat(values.get(1).getConsumption(), is(10));
        assertThat(values.get(2).getConsumption(), is(10));
        assertThat(values.get(3).getConsumption(), is(30));
        assertThat(values.get(4).getConsumption(), is(0));

    }

    private DispensingItem createDispensingItemWithDate(Commodity commodity, Date firstDate, int quantity) {
        Dispensing dispensing = new Dispensing();
        final DispensingItem dispensingItem = new DispensingItem(commodity, quantity);
        dispensingItem.setCreated(firstDate);
        dispensingItem.setDispensing(dispensing);
        dbUtil.withDao(DispensingItem.class, new DbUtil.Operation<DispensingItem, Object>() {
            @Override
            public Object operate(Dao<DispensingItem, String> dao) throws SQLException {
                return dao.create(dispensingItem);
            }
        });
        return dispensingItem;
    }

    public int daysBetween(Date d1, Date d2) {
        return Days.daysBetween(new DateTime(d1), new DateTime(d2)).getDays();
    }

    @Test
    public void shouldReturnCorrectNumberOfMonthlyVaccineUtilizationReportItems() throws Exception {
        Category category = categories.get(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.APRIL, 01);
        Date date = calendar.getTime();
        List<MonthlyVaccineUtilizationReportItem> reportItems = reportsService.getMonthlyVaccineUtilizationReportItems(category, dateFormatYear.format(date),
                dateFormatMonth.format(date));

        int expectedSize = category.getCommodities().size();
        assertThat(reportItems.size(), is(expectedSize));
    }
}