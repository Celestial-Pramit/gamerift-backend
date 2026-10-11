package com.gamerift.feature.payment.gateway;

import java.math.BigDecimal;
import java.util.Map;

import com.gamerift.feature.payment.model.Order;
import com.gamerift.feature.user.model.User;
import com.gamerift.shared.exception.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Talks to the SSLCommerz gateway (layer: gateway client, used by the payment service).
 *
 * Two jobs, matching the official "Hosted Checkout" flow:
 *  1. createSession(...) - ask SSLCommerz for a payment page URL and return it.
 *  2. validate(valId)    - ask SSLCommerz to confirm a payment really happened.
 *
 * We only ever send the credentials the caller configured; nothing is invented here.
 * The high-level rules (which order, which amount is correct) live in the service, not here.
 */
@Component
public class SslCommerzClient {

    private static final Logger log = LoggerFactory.getLogger(SslCommerzClient.class);

    private final SslCommerzProperties props;
    private final String publicUrl;
    private final RestClient restClient = RestClient.create();

    // Spring passes the settings bean plus the public URL from application.yml
    public SslCommerzClient(SslCommerzProperties props, @Value("${app.public-url}") String publicUrl) {
        this.props = props;
        this.publicUrl = publicUrl;
    }

    /**
     * Starts a payment session and returns the URL of the SSLCommerz hosted payment page.
     * All the mandatory fields are filled in here. The callback URLs point back to our own API.
     */
    public String createSession(Order order, User user) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("store_id", props.getStoreId());
        form.add("store_passwd", props.getStorePasswd());
        form.add("total_amount", BigDecimal.valueOf(order.getTotal()).setScale(2).toPlainString());
        form.add("currency", "BDT");
        form.add("tran_id", order.getId());
        form.add("success_url", publicUrl + "/api/payment/sslcommerz/success");
        form.add("fail_url", publicUrl + "/api/payment/sslcommerz/fail");
        form.add("cancel_url", publicUrl + "/api/payment/sslcommerz/cancel");
        form.add("ipn_url", publicUrl + "/api/payment/sslcommerz/ipn");
        form.add("cus_name", safe(user.getUsername()));
        form.add("cus_email", safe(user.getEmail()));
        form.add("cus_add1", "Dhaka");
        form.add("cus_city", "Dhaka");
        form.add("cus_country", safe(user.getCountry(), "Bangladesh"));
        form.add("cus_phone", "01000000000");
        form.add("product_name", safe(order.getPlanName(), "Gamerift Pro"));
        form.add("product_category", "Digital");
        form.add("product_profile", "general");
        form.add("shipping_method", "NO");
        form.add("num_of_item", "1");

        // Log the callback URLs being sent to SSLCommerz (never log credentials)
        String successUrl = publicUrl + "/api/payment/sslcommerz/success";
        String failUrl = publicUrl + "/api/payment/sslcommerz/fail";
        String cancelUrl = publicUrl + "/api/payment/sslcommerz/cancel";
        String ipnUrl = publicUrl + "/api/payment/sslcommerz/ipn";
        log.info("SSLCommerz session init: tran_id={}, total_amount={}, success_url={}, fail_url={}, cancel_url={}, ipn_url={}",
                order.getId(),
                BigDecimal.valueOf(order.getTotal()).setScale(2).toPlainString(),
                successUrl, failUrl, cancelUrl, ipnUrl);

        Map<String, Object> response = postForm(props.getBaseUrl() + "/gwprocess/v4/api.php", form);

        String status = text(response.get("status"));
        String gatewayUrl = text(response.get("GatewayPageURL"));
        if (!"SUCCESS".equalsIgnoreCase(status) || gatewayUrl == null || gatewayUrl.isBlank()) {
            // The gateway refused: log only the failedreason, then throw
            log.warn("SSLCommerz session init failed: {}", text(response.get("failedreason")));
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Payment gateway refused the request: " + text(response.get("failedreason")));
        }
        return gatewayUrl;
    }

    /**
     * Asks SSLCommerz to confirm a payment using its val_id.
     * The caller then compares the returned amount, currency and transaction id with our order.
     */
    public SslCommerzValidation validate(String valId) {
        Map<String, Object> response = restClient.get()
                .uri(props.getBaseUrl()
                                + "/validator/api/validationserverAPI.php"
                                + "?val_id={valId}&store_id={storeId}&store_passwd={storePasswd}&format=json",
                        valId, props.getStoreId(), props.getStorePasswd())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() { });

        SslCommerzValidation result = new SslCommerzValidation();
        if (response == null) {
            return result;
        }
        result.setStatus(text(response.get("status")));
        result.setTranId(text(response.get("tran_id")));
        result.setAmount(text(response.get("amount")));
        result.setCurrency(text(response.get("currency")));
        result.setBankTranId(text(response.get("bank_tran_id")));
        result.setCardType(text(response.get("card_type")));
        return result;
    }

    // Sends the form as application/x-www-form-urlencoded and reads the JSON answer.
    private Map<String, Object> postForm(String url, MultiValueMap<String, String> form) {
        Map<String, Object> response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() { });
        if (response == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment gateway returned an empty answer");
        }
        return response;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
