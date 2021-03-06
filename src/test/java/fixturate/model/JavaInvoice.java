/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fixturate.model;

import java.util.List;

public class JavaInvoice {
    private final Long number;
    private final List<JavaOrder> orders;
    private final JavaInvoiceType invoiceType;

    public JavaInvoice(Long number, List<JavaOrder> orders, JavaInvoiceType invoiceType) {
        this.number = number;
        this.orders = orders;
        this.invoiceType = invoiceType;
    }

    public Long getNumber() {
        return number;
    }

    public List<JavaOrder> getOrders() {
        return orders;
    }

    public JavaInvoiceType getInvoiceType() {
        return invoiceType;
    }
}
