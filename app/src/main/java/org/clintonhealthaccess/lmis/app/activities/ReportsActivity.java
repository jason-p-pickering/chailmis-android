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

package org.clintonhealthaccess.lmis.app.activities;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.inject.Inject;

import org.clintonhealthaccess.lmis.app.R;
import org.clintonhealthaccess.lmis.app.adapters.reports.ReportTypeAdapter;
import org.clintonhealthaccess.lmis.app.models.Category;
import org.clintonhealthaccess.lmis.app.models.ReportType;
import org.clintonhealthaccess.lmis.app.services.CategoryService;

import java.util.ArrayList;
import java.util.List;

import roboguice.inject.InjectView;

public class ReportsActivity extends BaseActivity {

    public static final String CATEGORY_BUNDLE_KEY = "Category";
    public static final String REPORT_TYPE_BUNDLE_KEY = "ReportType";

    @Inject
    CategoryService categoryService;

    @InjectView(R.id.layoutCategories)
    private LinearLayout categoriesLayout;

    @InjectView(R.id.listViewCategoryReports)
    private ListView listViewCategoryReports;

    @InjectView(R.id.relativeLayoutEmpty)
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.transparent);
        setContentView(R.layout.activity_reports);
        setupCategories();
    }

    private void setupCategories() {
        final ReportTypeAdapter adapter = new ReportTypeAdapter(getBaseContext(), R.layout.report_button, new ArrayList<ReportType>());
        listViewCategoryReports.setAdapter(adapter);

        listViewCategoryReports.setEmptyView(emptyView);
        Drawable commodityButtonBackground = createCommodityButtonBackground();
        for (final Category category : categoryService.all()) {
            final Button button = createCommoditySelectionButton(category, commodityButtonBackground);
            categoriesLayout.addView(button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final List<ReportType> reportTypeList = ReportType.getReportTypesForCategory(category.getName());
                    adapter.clear();
                    adapter.setCategory(category);
                    adapter.addAll(reportTypeList);

                    for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
                        TextView button = (TextView) categoriesLayout.getChildAt(i);
                        if (button.getText().toString().equalsIgnoreCase(category.getName())) {
                            button.setSelected(true);
                        } else {
                            button.setSelected(false);
                        }
                    }

                }
            });
        }


    }

    private Drawable createCommodityButtonBackground() {
        Drawable commodityButtonBackground = getResources().getDrawable(R.drawable.arrow_black_right);
        commodityButtonBackground.setBounds(0, 0, 20, 30);
        return commodityButtonBackground;
    }

    private Button createCommoditySelectionButton(final Category category, Drawable background) {
        Button button = new Button(this);
        button.setBackgroundResource(R.drawable.category_button_on_overlay);
        button.setCompoundDrawables(null, null, background, null);
        button.setText(category.getName());
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        return button;
    }

}
