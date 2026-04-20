import streamlit as st
import pandas as pd
import requests
from datetime import datetime, date, timedelta

st.set_page_config(page_title="Consumer Marketplace", page_icon="🛒", layout="wide")

st.title("🛒 Consumer Marketplace")
st.markdown("Browse fresh produce, join community group orders, and manage subscriptions.")

# --- Constants ---
BASE_URL = "http://localhost:8080/api"

PICKUP_AREAS   = ["City Center", "Suburbs", "North Hills", "Downtown", "East Ridge"]
PICKUP_SLOTS   = ["MORNING (7-10 AM)", "AFTERNOON (12-3 PM)", "EVENING (5-8 PM)"]
PAUSE_REASONS  = ["Vacation / Travel", "Budget constraints", "Already have stock", "Health reasons", "Other"]

# --- Consumer Profile Selector ---
CONSUMERS = {
    "Alice Consumer (ID 4)": {"id": 4, "name": "Alice"},
    "Bob Brown (ID 5)":      {"id": 5, "name": "Bob"},
    "Charlie Clark (ID 6)":  {"id": 6, "name": "Charlie"}
}
selected_consumer_label = st.sidebar.selectbox("👤 Select Consumer Profile", list(CONSUMERS.keys()))
CONSUMER_ID   = CONSUMERS[selected_consumer_label]["id"]
consumer_name = CONSUMERS[selected_consumer_label]["name"]

st.sidebar.header(f"Welcome, {consumer_name}!")

# --- API Helper Functions ---
def get_marketplace_items():
    try:
        r = requests.get(f"{BASE_URL}/marketplace/produce")
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []

def get_active_pools():
    try:
        r = requests.get(f"{BASE_URL}/marketplace/active-pools")
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []

def get_my_subscriptions():
    try:
        r = requests.get(f"{BASE_URL}/subscription/{CONSUMER_ID}/my-boxes")
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []

def get_my_orders():
    try:
        r = requests.get(f"{BASE_URL}/consumer/{CONSUMER_ID}/my-orders")
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []

def get_logistics_slots():
    try:
        r = requests.get(f"{BASE_URL}/logistics/slots")
        return r.json() if r.status_code == 200 else []
    except Exception:
        return []

def get_wallet_balance():
    try:
        r = requests.get(f"{BASE_URL}/consumer/{CONSUMER_ID}/wallet")
        return float(r.text) if r.status_code == 200 else 0.0
    except Exception:
        return 0.0

# --- Navigation ---
menu = st.sidebar.radio("Navigation", [
    "🏪 Marketplace & Community Cart",
    "📦 Weekly SubscriptionBox",
    "✅ Checkout & Slots"
])

# --- Wallet Sidebar ---
st.sidebar.divider()
st.sidebar.subheader("💳 Wallet")
wallet_balance = get_wallet_balance()
st.sidebar.metric("Balance", f"${wallet_balance:.2f}")

with st.sidebar.expander("➕ Top-up Wallet"):
    topup_amt = st.number_input("Amount ($)", min_value=5.0, step=5.0, key="topup_amount")
    if st.button("Add Funds", key="add_funds_btn"):
        res = requests.post(f"{BASE_URL}/consumer/{CONSUMER_ID}/wallet/topup",
                            params={"amount": topup_amt})
        if res.status_code == 200:
            st.success(f"✅ Added ${topup_amt:.2f} to wallet!")
            st.rerun()
        else:
            st.error("Top-up failed. Please try again.")

# =============================================================
# PAGE: MARKETPLACE & COMMUNITY CART
# =============================================================
if menu == "🏪 Marketplace & Community Cart":
    st.subheader("Available Produce & Community Pools")

    pools = get_active_pools()
    if pools:
        st.info(f"🎉 Found **{len(pools)}** active Community Pool(s)! Join one to help hit the MOQ threshold.")
        for pool in pools:
            batch       = pool.get("harvestBatch", {})
            produce     = batch.get("produceType", "Unknown Produce")
            pool_id     = pool.get("orderId", "?")
            target      = pool.get("targetMinimumOrder", 1)
            current     = pool.get("poolTotalQuantity", 0)
            progress    = current / target if target > 0 else 0

            with st.expander(f"🛒 Pool #{pool_id}: {produce} — {current:.0f}/{target:.0f} kg ({progress*100:.0f}% filled)"):
                col1, col2 = st.columns([3, 1])
                with col1:
                    st.write(f"**Target MOQ:** {target} kg | **Current Pool:** {current} kg")
                    st.progress(min(progress, 1.0))
                    if batch:
                        st.caption(f"Price: ${batch.get('basePrice', 0):.2f}/kg | Batch #{batch.get('batchId', '?')}")
                with col2:
                    qty = st.number_input("Qty (kg)", min_value=1.0, step=1.0, key=f"pool_qty_{pool_id}")
                    if st.button("Join Pool", key=f"pool_btn_{pool_id}"):
                        res = requests.post(f"{BASE_URL}/consumer/{CONSUMER_ID}/orders/{pool_id}/join",
                                            params={"quantity": qty})
                        if res.status_code == 200:
                            st.success("Successfully joined the pool!")
                            st.rerun()
                        else:
                            st.error(f"Failed to join: {res.text}")
    else:
        st.info("No active community pools right now. Start one from the produce list below!")

    st.divider()
    st.subheader("Discover Fresh Produce")
    items = get_marketplace_items()
    if not items:
        st.warning("No fresh produce currently available in the marketplace.")
    else:
        for item in items:
            with st.container():
                col1, col2, col3 = st.columns([2, 1, 1])
                with col1:
                    st.markdown(f"### {item['produceType']}")
                    st.caption(f"Batch ID: {item['batchId']}")
                    st.markdown(f"**Price:** ${item['basePrice']:.2f} / kg")
                with col2:
                    avail = item['totalQuantity'] - item['quantitySold']
                    st.metric("Available", f"{avail} kg")
                with col3:
                    moq_target = st.number_input("Target MOQ (kg)", min_value=10.0, step=5.0,
                                                  value=20.0, key=f"moq_{item['batchId']}")
                    if st.button("Start New Community Pool", key=f"start_{item['batchId']}"):
                        res = requests.post(f"{BASE_URL}/consumer/orders/start",
                                            params={"batchId": item["batchId"], "moq": moq_target})
                        if res.status_code == 200:
                            st.success(f"New pool started for {item['produceType']}!")
                            st.rerun()
                        else:
                            st.error("Failed to start pool. Check backend logs.")

# =============================================================
# PAGE: WEEKLY SUBSCRIPTION BOX
# =============================================================
elif menu == "📦 Weekly SubscriptionBox":
    st.subheader("📦 Manage Weekly Subscription Box")

    my_subs    = get_my_subscriptions()
    active_sub = next((s for s in my_subs if s.get("active")), None)

    # ---- Top banner: Activate / Status toggle ----
    col_toggle, col_hint = st.columns([1, 3])
    with col_toggle:
        sub_active = st.toggle("Activate Subscription", value=active_sub is not None)
    with col_hint:
        if active_sub:
            paused = active_sub.get("paused", False)
            if paused:
                st.warning(f"⏸️ Subscription is **PAUSED** until {active_sub.get('pausedUntil')} — {active_sub.get('pausedReason', '')}")
            else:
                st.success(f"✅ Subscription is **ACTIVE** — {active_sub.get('boxDescription', '')}")

    # ---- CREATE NEW SUBSCRIPTION ----
    if sub_active and not active_sub:
        st.divider()
        st.markdown("### 🆕 Create New Subscription")

        with st.form("new_sub_form", clear_on_submit=False):
            c1, c2 = st.columns(2)
            with c1:
                box_type   = st.selectbox("📦 Box Type", ["VEGGIE", "FRUIT"],
                                           help="VEGGIE = vegetables; FRUIT = seasonal fruits")
                frequency  = st.selectbox("🔁 Delivery Frequency", ["WEEKLY", "BIWEEKLY", "MONTHLY"])
            with c2:
                pickup_area = st.selectbox("📍 Pickup Area", ["-- Select --"] + PICKUP_AREAS)
                pickup_slot = st.selectbox("⏰ Preferred Time Slot", ["-- Select --"] + PICKUP_SLOTS)

            preference = st.text_input("🥗 Produce Preferences",
                                        placeholder="e.g. No cilantro, only organic, local produce only")

            st.markdown("""
            | Box Type | Weekly Price | Contents |
            |----------|-------------|----------|
            | 🥦 VEGGIE | ~$15.99 | Seasonal vegetables, root veg, leafy greens |
            | 🍎 FRUIT  | ~$12.99 | Mix of seasonal fruits, exotic selections |
            """)

            submitted = st.form_submit_button("🚀 Subscribe Now", type="primary")
            if submitted:
                if pickup_area == "-- Select --" or pickup_slot == "-- Select --":
                    st.error("Please select a pickup area and time slot.")
                else:
                    slot_clean = pickup_slot.split(" ")[0]  # e.g. "MORNING"
                    res = requests.post(f"{BASE_URL}/subscription/{CONSUMER_ID}/create",
                                        params={
                                            "boxType": box_type,
                                            "frequency": frequency,
                                            "preference": preference,
                                            "pickupArea": pickup_area,
                                            "pickupTimeSlot": slot_clean
                                        })
                    if res.status_code == 200:
                        st.success("🎉 Subscription created! Your first box is on its way.")
                        st.rerun()
                    else:
                        st.error(f"Failed to create subscription: {res.text}")

    # ---- MANAGE EXISTING SUBSCRIPTION ----
    elif sub_active and active_sub:
        sub_id     = active_sub.get("subscriptionId")
        sub_price  = active_sub.get("pricePerCycle", 0)
        paused     = active_sub.get("paused", False)
        next_del   = active_sub.get("nextDeliveryDate", "N/A")
        deliveries = active_sub.get("totalDeliveriesCompleted", 0)
        pickup_a   = active_sub.get("pickupArea") or "Not set"
        pickup_t   = active_sub.get("pickupTimeSlot") or "Not set"
        pref       = active_sub.get("veggiePreference") or active_sub.get("fruitPreference") or "None set"

        # ---- Summary Cards ----
        st.divider()
        mc1, mc2, mc3, mc4 = st.columns(4)
        mc1.metric("📦 Box Type",         active_sub.get("boxType", "N/A"))
        mc2.metric("🔁 Frequency",        active_sub.get("frequency", "N/A"))
        mc3.metric("💰 Price per Cycle",  f"${sub_price:.2f}")
        mc4.metric("📅 Next Delivery",    next_del)

        bc1, bc2, bc3 = st.columns(3)
        bc1.metric("📍 Pickup Area",       pickup_a)
        bc2.metric("⏰ Time Slot",          pickup_t)
        bc3.metric("🏆 Deliveries Done",   deliveries)

        # ---- Wallet Balance Alert ----
        st.divider()
        if wallet_balance < sub_price:
            st.error(f"⚠️ **Insufficient Wallet Balance!** Your balance (${wallet_balance:.2f}) is below "
                     f"the next cycle cost (${sub_price:.2f}). Please top-up via the sidebar wallet.")
            col_notify, col_gap = st.columns([1, 3])
            with col_notify:
                if st.button("📧 Send Email Alert (Mock)", help="Sends a mock notification email about low balance"):
                    notify_res = requests.post(f"{BASE_URL}/consumer/{CONSUMER_ID}/wallet/notify-insufficient")
                    if notify_res.status_code == 200:
                        st.success("✅ Email notification sent successfully!")
                    else:
                        st.error("Failed to send notification.")
        else:
            st.success(f"✅ Wallet funded (${wallet_balance:.2f}) — fully covers next cycle (${sub_price:.2f}).")

        # ---- Preferences ----
        st.divider()
        with st.expander("🥗 Produce Preferences", expanded=False):
            st.write(f"**Current:** {pref}")

        # ---- Update Pickup Preferences ----
        st.divider()
        with st.expander("📍 Update Pickup Area & Time Slot", expanded=False):
            up1, up2 = st.columns(2)
            with up1:
                default_area = PICKUP_AREAS.index(pickup_a) if pickup_a in PICKUP_AREAS else 0
                new_pickup_area = st.selectbox("Pickup Area", PICKUP_AREAS, index=default_area,
                                                key="upd_pickup_area")
            with up2:
                slot_names = [s.split(" ")[0] for s in PICKUP_SLOTS]
                default_slot = slot_names.index(pickup_t) if pickup_t in slot_names else 0
                new_pickup_slot_full = st.selectbox("Time Slot", PICKUP_SLOTS, index=default_slot,
                                                     key="upd_pickup_slot")
                new_pickup_slot = new_pickup_slot_full.split(" ")[0]

            if st.button("💾 Save Pickup Preferences", key="save_pickup"):
                res = requests.patch(f"{BASE_URL}/subscription/{sub_id}/pickup",
                                     params={"pickupArea": new_pickup_area,
                                             "pickupTimeSlot": new_pickup_slot})
                if res.status_code == 200:
                    st.success(f"✅ Pickup updated: {new_pickup_area} — {new_pickup_slot_full}")
                    st.rerun()
                else:
                    st.error(f"Failed to update pickup: {res.text}")

        # ---- Pause / Resume ----
        st.divider()
        if paused:
            st.warning(f"⏸️ Subscription is paused until **{active_sub.get('pausedUntil')}**. "
                       f"Reason: _{active_sub.get('pausedReason', 'N/A')}_")
            if st.button("▶️ Resume Subscription Now", key="resume_sub"):
                res = requests.post(f"{BASE_URL}/subscription/{sub_id}/resume")
                if res.status_code == 200:
                    st.success("✅ Subscription resumed!")
                    st.rerun()
                else:
                    st.error(f"Failed to resume: {res.text}")
        else:
            with st.expander("⏸️ Pause Subscription", expanded=False):
                st.caption("Temporarily pause your subscription. Deliveries will be skipped automatically.")
                pause_col1, pause_col2 = st.columns(2)
                with pause_col1:
                    min_pause     = date.today() + timedelta(days=1)
                    default_pause = date.today() + timedelta(days=7)
                    pause_until   = st.date_input("Pause Until", value=default_pause,
                                                   min_value=min_pause, key="pause_until_date")
                with pause_col2:
                    pause_reason = st.selectbox("Reason", PAUSE_REASONS, key="pause_reason_sel")

                if st.button("⏸️ Pause Subscription", key="pause_sub_btn"):
                    res = requests.post(f"{BASE_URL}/subscription/{sub_id}/pause",
                                        params={"pauseUntil": pause_until.isoformat(),
                                                "reason": pause_reason})
                    if res.status_code == 200:
                        st.success(f"✅ Subscription paused until {pause_until}.")
                        st.rerun()
                    else:
                        st.error(f"Failed to pause: {res.text}")

        # ---- Subscription History ----
        st.divider()
        with st.expander("📊 Subscription Summary", expanded=False):
            df = pd.DataFrame([{
                "Field": "Box ID",          "Value": sub_id},
                {"Field": "Type",           "Value": active_sub.get("boxType")},
                {"Field": "Frequency",      "Value": active_sub.get("frequency")},
                {"Field": "Preferences",    "Value": pref},
                {"Field": "Pickup Area",    "Value": pickup_a},
                {"Field": "Pickup Slot",    "Value": pickup_t},
                {"Field": "Next Delivery",  "Value": next_del},
                {"Field": "Deliveries Done","Value": deliveries},
                {"Field": "Price / Cycle",  "Value": f"${sub_price:.2f}"},
                {"Field": "Status",         "Value": "⏸️ Paused" if paused else "✅ Active"},
            ])
            st.dataframe(df, use_container_width=True, hide_index=True)

        # ---- Cancel Subscription ----
        st.divider()
        with st.expander("❌ Cancel Subscription", expanded=False):
            st.warning("⚠️ This action will permanently cancel your subscription.")
            confirm = st.checkbox("I understand this cannot be undone")
            if confirm and st.button("Cancel Subscription", type="secondary", key="cancel_sub"):
                res = requests.delete(f"{BASE_URL}/subscription/{sub_id}")
                if res.status_code == 200:
                    st.warning("Subscription cancelled.")
                    st.rerun()

    else:
        st.info("Enable the toggle above to set up your weekly subscription box.")

        st.divider()
        st.markdown("#### 🌟 Why Subscribe?")
        col1, col2, col3 = st.columns(3)
        with col1:
            st.markdown("""
**🥦 VEGGIE Box**
- Fresh seasonal vegetables
- Root vegetables  
- Leafy greens
- Local organic options
- ~$15.99/week
""")
        with col2:
            st.markdown("""
**🍎 FRUIT Box**
- Seasonal fresh fruits
- Exotic selections
- Citrus & berries
- Farm-picked quality
- ~$12.99/week
""")
        with col3:
            st.markdown("""
**✨ Subscription Benefits**
- Skip any week anytime
- Choose pickup zone & slot
- Auto-renewal reminders
- Wallet auto-deduction
- Delivery history tracking
""")

# =============================================================
# PAGE: CHECKOUT & SLOTS
# =============================================================
elif menu == "✅ Checkout & Slots":
    st.subheader("Checkout & Delivery Booking")

    my_orders = get_my_orders()

    if not my_orders:
        st.warning("You haven't joined any community pools yet. Go to the Marketplace to join one!")
    else:
        st.markdown("### 1. Select Your Order")
        order_options = {}
        for o in my_orders:
            batch        = o.get("harvestBatch", {})
            produce_name = batch.get("produceType", "Unknown") if batch else "Unknown"
            label        = f"Order #{o['orderId']} - {produce_name} (Status: {o['status']})"
            order_options[label] = o

        selected_order_label = st.selectbox("Your Active Orders", options=list(order_options.keys()))
        selected_order       = order_options[selected_order_label]

        # Calculate amount
        batch          = selected_order.get("harvestBatch", {})
        price_per_kg   = batch.get("basePrice", 0) if batch else 0
        total_qty      = selected_order.get("poolTotalQuantity", 0)
        estimated_amt  = price_per_kg * total_qty

        st.info(f"Estimated Total for this Community Pool: **${estimated_amt:.2f}**")

        st.markdown("### 2. Select a Delivery Slot")
        available_slots = get_logistics_slots()

        if not available_slots:
            st.error("No delivery slots available currently.")
        else:
            slot_options = {}
            for s in available_slots:
                remaining = s["maxCapacity"] - s.get("currentBookings", s.get("current_bookings", 0))
                label     = f"{s['slotTime']} — {s['zone']} ({remaining} spots left)"
                slot_options[label] = s["slotId"]

            selected_slot_label = st.selectbox("Available Logistics Slots",
                                                options=list(slot_options.keys()))
            selected_slot_id = slot_options[selected_slot_label]

            st.divider()
            st.markdown("### 3. Payment Method")
            pay_method = st.radio("Payment Type", ["Wallet", "UPI", "Credit Card"], horizontal=True)

            if pay_method == "Wallet":
                wc1, wc2 = st.columns(2)
                wc1.metric("💳 Wallet Balance",  f"${wallet_balance:.2f}")
                wc2.metric("🛒 Order Total",     f"${estimated_amt:.2f}")

                if wallet_balance < estimated_amt:
                    shortage = estimated_amt - wallet_balance
                    st.error(
                        f"⚠️ **Insufficient Wallet Balance!** "
                        f"You need **${shortage:.2f}** more. "
                        f"Please top-up your wallet via the sidebar."
                    )
                    st.info("💡 Tip: Switch to UPI or Credit Card, or top-up your wallet from the sidebar.")
                else:
                    remaining_after = wallet_balance - estimated_amt
                    st.success(f"✅ Sufficient balance! After payment, your wallet will have **${remaining_after:.2f}**.")

            can_checkout = not (pay_method == "Wallet" and wallet_balance < estimated_amt)

            if st.button("✅ Confirm Order & Book Delivery", type="primary",
                         disabled=not can_checkout):
                payload = {
                    "orderId":       selected_order["orderId"],
                    "slotId":        selected_slot_id,
                    "consumerId":    CONSUMER_ID,
                    "amount":        estimated_amt,
                    "paymentMethod": pay_method
                }
                res = requests.post(f"{BASE_URL}/consumer/checkout", json=payload)
                if res.status_code == 200:
                    st.balloons()
                    st.success("🎉 Payment Successful! Delivery slot booked.")
                    st.markdown("Your order status is now **FULFILLED**. Check the Marketplace tab.")
                else:
                    st.error(f"Checkout Failed: {res.text}")

            if not can_checkout:
                st.caption("🔴 Checkout is disabled because your wallet balance is insufficient.")
