package fixturate.model;

import org.junit.Test;
import static dridco.tests.fixturate.java.Fixturate.*;
import static org.junit.Assert.assertEquals;

public class FixturateTest {

    @Test
    public void sampleFixtureUsage() throws Exception {
        JavaInvoice invoice = fixture(JavaInvoice.class).in("invoice for john and jane").get();
        assertEquals(Long.valueOf(1), invoice.getNumber());
        assertEquals(2, invoice.getOrders().size());
    }

    @Test
    public void shouldRetrieveInvoiceWithEnum() throws Exception {
        JavaInvoice invoice = fixture(JavaInvoice.class).in("invoice for john and jane").get();
        assertEquals(JavaInvoiceType.INVOICE, invoice.getInvoiceType());
    }



}
