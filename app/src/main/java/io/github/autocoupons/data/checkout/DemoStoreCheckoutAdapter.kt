package io.github.autocoupons.data.checkout

import io.github.autocoupons.core.web.WebPageBridge
import io.github.autocoupons.data.coupon.DemoCouponProvider
import io.github.autocoupons.domain.coupon.ApplyObservation
import io.github.autocoupons.domain.coupon.CartSnapshot
import io.github.autocoupons.domain.coupon.CheckoutAdapter
import io.github.autocoupons.domain.coupon.CheckoutUnavailableException
import io.github.autocoupons.domain.coupon.Money
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Checkout automation for the bundled Demo Store, written the way a real retailer adapter is:
 * it types into the coupon field, presses the store's Apply button, waits for the store's own
 * status element to settle and then reads the totals the page displays. It never touches the
 * (disabled) Place Order button.
 */
class DemoStoreCheckoutAdapter(private val bridge: WebPageBridge) : CheckoutAdapter {

    override val retailerId: String = DemoCouponProvider.DEMO_STORE_ID

    override suspend fun readCart(): CartSnapshot = cartFrom(invoke("read()"))

    override suspend fun applyCoupon(code: String): ApplyObservation {
        val response = invoke("apply(${JsonPrimitive(code)})")
        val cart = cartFrom(response)
        val message = response["message"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        return when (response["state"]?.jsonPrimitive?.contentOrNull) {
            "applied" -> ApplyObservation.Accepted(cart, message)
            "rejected" -> ApplyObservation.Rejected(cart, message)
            else -> ApplyObservation.TimedOut
        }
    }

    override suspend fun removeCoupon(): CartSnapshot = cartFrom(invoke("remove()"))

    /** Returns true when the current page exposes the store's coupon entry and order summary. */
    suspend fun hasCouponEntry(): Boolean =
        invoke("probe()")["checkout"]?.jsonPrimitive?.booleanOrNull == true

    private suspend fun invoke(operation: String): JsonObject {
        val response = bridge.call { id -> "$ADAPTER_SCRIPT\nwindow.__acDemoAdapter.run(${JsonPrimitive(id)}, function(a){ return a.$operation; });" }
        if (response["ok"]?.jsonPrimitive?.booleanOrNull != true) {
            throw CheckoutUnavailableException(response["error"]?.jsonPrimitive?.contentOrNull ?: "Checkout page error")
        }
        return response
    }

    private fun cartFrom(response: JsonObject): CartSnapshot {
        val cart = response["cart"]?.jsonObject ?: throw CheckoutUnavailableException("Cart totals missing")
        val currency = cart["currency"]?.jsonPrimitive?.contentOrNull ?: Money.INR
        fun amount(key: String): Money {
            val minor = cart[key]?.jsonPrimitive?.longOrNull ?: throw CheckoutUnavailableException("Missing $key")
            return Money(minor, currency)
        }
        return CartSnapshot(
            subtotal = amount("subtotal"),
            discount = amount("discount"),
            total = amount("total"),
            appliedCode = cart["appliedCode"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
            fingerprint = cart["fingerprint"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
    }

    private companion object {
        /**
         * Installed once per page. Selectors are specific to cart.html; a missing element is an
         * error rather than a guess. Waits use a MutationObserver plus an 8 s ceiling (no polling).
         */
        val ADAPTER_SCRIPT = """
            (function () {
              if (window.__acDemoAdapter) return;
              function q(sel) { var el = document.querySelector(sel); if (!el) throw new Error('Missing ' + sel); return el; }
              function minor(sel) {
                var v = parseInt(q(sel).getAttribute('data-minor'), 10);
                if (isNaN(v)) throw new Error('Unreadable ' + sel);
                return v;
              }
              function cart() {
                var lines = Array.prototype.map.call(document.querySelectorAll('#cart-items [data-sku]'), function (e) {
                  return e.getAttribute('data-sku') + 'x' + e.getAttribute('data-qty');
                }).sort().join('|');
                var code = q('#applied-coupon').getAttribute('data-code');
                return {
                  subtotal: minor('#summary-subtotal'), discount: minor('#summary-discount'), total: minor('#summary-total'),
                  currency: q('#summary-total').getAttribute('data-currency') || 'INR',
                  appliedCode: code ? code : null, fingerprint: lines
                };
              }
              function settle(previousSeq, timeoutMs) {
                return new Promise(function (resolve) {
                  var el = q('#coupon-message');
                  var timer = null;
                  var observer = new MutationObserver(check);
                  function check() {
                    var state = el.getAttribute('data-state');
                    if (el.getAttribute('data-seq') !== previousSeq && state !== 'pending') {
                      observer.disconnect(); clearTimeout(timer); resolve(state);
                    }
                  }
                  observer.observe(el, { attributes: true, attributeFilter: ['data-state', 'data-seq'] });
                  timer = setTimeout(function () { observer.disconnect(); resolve('timeout'); }, timeoutMs);
                  check();
                });
              }
              function reply(id, payload) {
                payload.id = id;
                AutoCouponsBridge.postMessage(JSON.stringify(payload));
              }
              var api = {
                read: function () { return { ok: true, cart: cart() }; },
                probe: function () {
                  return { ok: true, checkout: !!(document.querySelector('#coupon-input') && document.querySelector('#summary-total')) };
                },
                apply: function (code) {
                  var seq = q('#coupon-message').getAttribute('data-seq');
                  var input = q('#coupon-input');
                  input.value = code;
                  input.dispatchEvent(new Event('input', { bubbles: true }));
                  q('#coupon-apply').click();
                  return settle(seq, 8000).then(function (state) {
                    return { ok: true, state: state, message: q('#coupon-message').textContent.trim(), cart: cart() };
                  });
                },
                remove: function () {
                  var box = q('#applied-coupon');
                  if (box.hidden || !box.getAttribute('data-code')) return { ok: true, cart: cart() };
                  var seq = q('#coupon-message').getAttribute('data-seq');
                  q('#coupon-remove').click();
                  return settle(seq, 8000).then(function () { return { ok: true, cart: cart() }; });
                }
              };
              window.__acDemoAdapter = {
                run: function (id, op) {
                  try {
                    Promise.resolve(op(api)).then(function (r) { reply(id, r); }, function (e) {
                      reply(id, { ok: false, error: String(e && e.message || e) });
                    });
                  } catch (e) {
                    reply(id, { ok: false, error: String(e && e.message || e) });
                  }
                }
              };
            })();
        """.trimIndent()
    }
}
