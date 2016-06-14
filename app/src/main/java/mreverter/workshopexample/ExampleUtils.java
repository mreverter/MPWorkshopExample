package mreverter.workshopexample;

import com.mercadopago.model.Item;

import java.math.BigDecimal;

/**
 * Created by mreverter on 13/6/16.
 */
public class ExampleUtils {
    public static String BASE_URL = "http://private-9376e-paymentmethodsmla.apiary-mock.com";
    public static String CREATE_PAYMENT_URI = "create_payment";
    public static BigDecimal AMOUT = new BigDecimal(100);
    public static Item ITEM = new Item("remera_pulpo", 2);
    public static String PUBLIC_KEY = "TEST-ad365c37-8012-4014-84f5-6c895b3f8e0a";
}
