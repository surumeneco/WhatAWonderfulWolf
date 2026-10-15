package co.surumene.www.util;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WolfDisplayNameTest {
 @Test void unnamedUsesFallback() {
  Component fallback = Component.text("fallback");
  assertSame(fallback, WolfDisplayName.resolve(null, fallback));
 }
}
