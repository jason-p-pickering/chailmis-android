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

package org.clintonhealthaccess.lmis.app.activities.viewmodels;

import org.clintonhealthaccess.lmis.app.models.AllocationItem;
import org.clintonhealthaccess.lmis.app.models.Commodity;
import org.clintonhealthaccess.lmis.app.models.ReceiveItem;

import lombok.Getter;
import lombok.Setter;

public class ReceiveCommodityViewModel extends BaseCommodityViewModel {

    private boolean quantityAllocatedDisabled = false;
    private int quantityAllocated;
    private int quantityReceived;

    public ReceiveCommodityViewModel(Commodity commodity) {
        super(commodity);
    }

    public ReceiveCommodityViewModel(Commodity commodity, int quantityAllocated, int quantityReceived) {
        super(commodity);
        this.quantityAllocated = quantityAllocated;
        this.quantityReceived = quantityReceived;
    }

    public ReceiveCommodityViewModel(AllocationItem item) {
        super(item.getCommodity());
        this.quantityAllocated = item.getQuantity();
        this.quantityAllocatedDisabled = true;
    }

    public int getDifference() {
        return quantityReceived - quantityAllocated;
    }

    public ReceiveItem getReceiveItem() {
        ReceiveItem receiveItem = new ReceiveItem(this.getCommodity(), quantityAllocated, quantityReceived);
        return receiveItem;
    }

    public boolean isQuantityAllocatedDisabled() {
        return quantityAllocatedDisabled;
    }

    public int getQuantityAllocated(){
        return quantityAllocated;
    }

    public int getQuantityReceived(){
        return quantityReceived;
    }

    public void setQuantityAllocated(int quantityAllocated) {
        this.quantityAllocated = quantityAllocated;
    }

    public void setQuantityReceived(int quantityReceived) {
        this.quantityReceived = quantityReceived;
    }
}
