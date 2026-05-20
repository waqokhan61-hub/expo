package expo.modules.appmetrics

import expo.modules.appmetrics.utils.JsonAny
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class GlobalAttributesTest {
  @Before
  fun resetBefore() {
    GlobalAttributes.set(null)
  }

  @After
  fun resetAfter() {
    GlobalAttributes.set(null)
  }

  @Test
  fun `merged returns event attributes when store is empty`() {
    val merged = GlobalAttributes.merged(mapOf("userId" to "u_42"))
    assertNotNull(merged)
    assertEquals(1, merged!!.size)
    assertEquals("u_42", merged["userId"])
  }

  @Test
  fun `merged returns null when both store and event attributes are empty`() {
    assertNull(GlobalAttributes.merged(null))
  }

  @Test
  fun `set populates the store and merged returns globals`() {
    GlobalAttributes.set(mapOf("tier" to "pro", "variant" to "B"))
    val merged = GlobalAttributes.merged(null)
    assertNotNull(merged)
    assertEquals(2, merged!!.size)
    assertEquals("pro", merged["tier"])
    assertEquals("B", merged["variant"])
  }

  @Test
  fun `merged combines globals and per-event attributes`() {
    GlobalAttributes.set(mapOf("tier" to "pro"))
    val merged = GlobalAttributes.merged(mapOf("screen" to "home"))
    assertNotNull(merged)
    assertEquals(2, merged!!.size)
    assertEquals("pro", merged["tier"])
    assertEquals("home", merged["screen"])
  }

  @Test
  fun `per-event attributes win on key collision`() {
    GlobalAttributes.set(mapOf("tier" to "pro"))
    val merged = GlobalAttributes.merged(mapOf("tier" to "trial"))
    assertNotNull(merged)
    assertEquals(1, merged!!.size)
    assertEquals("trial", merged["tier"])
  }

  @Test
  fun `set replaces the previous store (not merges)`() {
    GlobalAttributes.set(mapOf("tier" to "pro", "variant" to "B"))
    GlobalAttributes.set(mapOf("tier" to "trial"))
    val merged = GlobalAttributes.merged(null)
    assertNotNull(merged)
    assertEquals(1, merged!!.size)
    assertEquals("trial", merged["tier"])
    assertNull(merged["variant"])
  }

  @Test
  fun `set with empty map clears the store`() {
    GlobalAttributes.set(mapOf("tier" to "pro"))
    GlobalAttributes.set(emptyMap())
    assertNull(GlobalAttributes.merged(null))
  }

  @Test
  fun `set with null clears the store`() {
    GlobalAttributes.set(mapOf("tier" to "pro"))
    GlobalAttributes.set(null)
    assertNull(GlobalAttributes.merged(null))
  }

  @Test
  fun `set sanitizes reserved keys before storing`() {
    GlobalAttributes.set(
      mapOf(
        "expo.app.name" to "spoofed",
        "session.id" to "spoofed",
        "tier" to "pro"
      )
    )
    val merged = GlobalAttributes.merged(null)
    assertNotNull(merged)
    assertEquals(1, merged!!.size)
    assertEquals("pro", merged["tier"])
  }

  @Test
  fun `mergedJson returns input unchanged when store is empty`() {
    val json = JsonAny.encodeMapToJsonString(mapOf("screen" to "home"))
    assertEquals(json, GlobalAttributes.mergedJson(json))
  }

  @Test
  fun `mergedJson returns null when input is null and store is empty`() {
    assertNull(GlobalAttributes.mergedJson(null))
  }

  @Test
  fun `mergedJson folds globals into the JSON map`() {
    GlobalAttributes.set(mapOf("tier" to "pro"))
    val input = JsonAny.encodeMapToJsonString(mapOf("screen" to "home"))
    val merged = JsonAny.decodeJsonStringToMap(GlobalAttributes.mergedJson(input)!!)!!
    assertEquals(2, merged.size)
    assertEquals("home", merged["screen"])
    assertEquals("pro", merged["tier"])
  }

  @Test
  fun `mergedJson encodes globals when input is null`() {
    GlobalAttributes.set(mapOf("tier" to "pro"))
    val merged = JsonAny.decodeJsonStringToMap(GlobalAttributes.mergedJson(null)!!)!!
    assertEquals(1, merged.size)
    assertEquals("pro", merged["tier"])
  }

  @Test
  fun `mergedJson returns input unchanged when JSON is unparseable`() {
    GlobalAttributes.set(mapOf("tier" to "pro"))
    val bad = "not json"
    assertEquals(bad, GlobalAttributes.mergedJson(bad))
  }
}
