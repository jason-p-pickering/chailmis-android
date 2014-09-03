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

package org.clintonhealthaccess.lmis.app.remote.endpoints;

import org.clintonhealthaccess.lmis.app.LmisException;
import org.clintonhealthaccess.lmis.app.models.DataSet;
import org.clintonhealthaccess.lmis.app.models.UserProfile;
import org.clintonhealthaccess.lmis.app.models.api.DataElement;
import org.clintonhealthaccess.lmis.app.models.api.DataElementGroup;
import org.clintonhealthaccess.lmis.app.models.api.DataElementGroupSet;
import org.clintonhealthaccess.lmis.app.models.api.DataValueSet;
import org.clintonhealthaccess.lmis.app.models.api.OptionSetResponse;
import org.clintonhealthaccess.lmis.app.remote.responses.DataSetSearchResponse;

import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

public interface Dhis2Endpoint {
    @GET("/api/systemSettings/data_element_group_set_id")
    String getDateElementGroupSetId();

    @GET("/api/me")
    UserProfile validateLogin() throws LmisException;

    @GET("/api/dataSets/{id}")
    DataSet getDataSet(@Path("id") String id);

    @GET("/api/dataElements/{id}")
    DataElement getDataElement(@Path("id") String id);


    @GET("/api/dataElementGroupSets/{id}")
    DataElementGroupSet getDataElementGroupSet(@Path("id") String id);

    @GET("/api/dataElementGroups/{id}")
    DataElementGroup getDataElementGroup(@Path("id") String id);

    @GET("/api/dataSets")
    DataSetSearchResponse searchDataSets(@Query("query") String query, @Query("fields") String fields);

    @GET("/api/dataValueSets")
    DataValueSet fetchDataValues(@Query("dataSet") String dataSet, @Query("orgUnit") String orgUnit, @Query("startDate") String startDate, @Query("endDate") String endDate);

    @GET("/api/optionSets")
    OptionSetResponse searchOptionSets(@Query("query") String query, @Query("fields") String fields);

}
  