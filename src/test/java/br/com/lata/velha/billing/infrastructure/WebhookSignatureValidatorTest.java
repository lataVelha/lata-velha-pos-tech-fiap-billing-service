package br.com.lata.velha.billing.infrastructure;

import br.com.lata.velha.billing.infrastructure.mercadopago.WebhookSignatureValidator;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureValidatorTest {

    // Segredos de teste: strings hexadecimais de 64 chars (32 bytes), igual ao
    // formato que o painel do Mercado Pago gera e que o WebhookSignatureValidator
    // espera receber (hex-decoded como chave HMAC-SHA256).
    private static final String SECRET       = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef";
    private static final String OTHER_SECRET = "cafebabecafebabecafebabecafebabecafebabecafebabecafebabecafebabe";

    static String assinar(String secret, String manifesto) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(HexFormat.of().parseHex(secret), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifesto.getBytes(StandardCharsets.UTF_8)));
    }

    private final WebhookSignatureValidator validator = new WebhookSignatureValidator(SECRET);

    @Test
    void aceitaAssinaturaCorreta() throws Exception {
        var hash = assinar(SECRET, "id:123;request-id:req-1;ts:1704908010;");
        assertThat(validator.valido("ts=1704908010,v1=" + hash, "req-1", "123")).isTrue();
    }

    @Test
    void dataIdAlfanumericoEhComparadoEmMinusculas() throws Exception {
        var hash = assinar(SECRET, "id:abc12;request-id:req-1;ts:1;");
        assertThat(validator.valido("ts=1, v1=" + hash, "req-1", "ABC12")).isTrue();
    }

    @Test
    void omiteCamposAusentesDoManifesto() throws Exception {
        var hash = assinar(SECRET, "id:123;ts:1;");
        assertThat(validator.valido("ts=1,v1=" + hash, null, "123")).isTrue();
    }

    @Test
    void rejeitaAssinaturaInvalidaOuMalformada() throws Exception {
        var hash = assinar(OTHER_SECRET, "id:123;request-id:req-1;ts:1;");
        assertThat(validator.valido("ts=1,v1=" + hash, "req-1", "123")).isFalse();
        assertThat(validator.valido("ts=1,v1=" + assinar(SECRET, "id:123;request-id:req-1;ts:1;"), "req-1", "999")).isFalse();
        assertThat(validator.valido(null, "req-1", "123")).isFalse();
        assertThat(validator.valido("v1=abc", "req-1", "123")).isFalse();
        assertThat(validator.valido("lixo", "req-1", "123")).isFalse();
    }

    @Test
    void semSegredoValidacaoDesligada() {
        var semSegredo = new WebhookSignatureValidator("");
        assertThat(semSegredo.habilitado()).isFalse();
        assertThat(semSegredo.valido(null, null, null)).isTrue();
    }
}
