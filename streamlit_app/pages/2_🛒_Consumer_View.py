import streamlit as st
import pandas as pd
import requests

st.set_page_config(page_title="Consumer Marketplace", page_icon="🛒", layout="wide")

st.title("🛒 Consumer Marketplace")
st.markdown("Browse fresh produce, join community group orders, and manage subscriptions.")

# --- Constants ---
BASE_URL = "http://localhost:8080/api"

# --- Consumer Profile Selector ---
CONSUMERS = {
    "Alice Consumer (ID 4)": {"id": 4, "name": "Alice"},
    "Bob Brown (ID 5)": {"id": 5, "name": "Bob"},
    "Charlie Clark (ID 6)": {"id": 6, "name": "Charlie"}
}
selected_consumer_label = st.sidebar.selectbox("Select Consumer Profile", list(CONSUMERS.keys()))
CONSUMER_ID = CONSUMERS[selected_consumer_label]["id"]
consumer_name = CONSUMERS[selected_consumer_label]["name"]

st.sidebar.header(f"Welcome, {consumer_name}!")

# --- API Helper Functions ---
def get_marketplace_items():
    try:
        response = requests.get(f"{BASE_URL}/marketplace/produce")
        if response.status_code == 200:
            return response.json()
        return []
    except Exception:
        return []

def get_active_pools():
    try:
        response = requests.get(f"{BASE_URL}/marketplace/active-pools")
        if response.status_code == 200:
            return response.json()
        return []
    except Exception:
        return []

def get_my_subscriptions():
    try:
        response = requests.get(f"{BASE_URL}/subscription/{CONSUMER_ID}/my-boxes")
        if response.status_code == 200:
            return response.json()
        return []
    except Exception:
        return []

def get_my_orders():
    try:
        response = requests.get(f"{BASE_URL}/consumer/{CONSUMER_ID}/my-orders")
        if response.status_code == 200:
            return response.json()
        return []
    except Exception:
        return []

def get_logistics_slots():
    try:
        response = requests.get(f"{BASE_URL}/logistics/slots")
        if response.status_code == 200:
            return response.json()
        return []
    except Exception:
        return []

def get_wallet_balance():
    try:
        response = requests.get(f"{BASE_URL}/consumer/{CONSUMER_ID}/wallet")
        if response.status_code == 200:
            return float(response.text)
        return 0.0
    except Exception:
        return 0.0

# --- Navigation ---
menu = st.sidebar.radio("Navigation", ["Marketplace & Community Cart", "Weekly SubscriptionBox", "Checkout & Slots"])

st.sidebar.divider()
st.sidebar.subheader("💳 Wallet")
wallet_balance = get_wallet_balance()
st.sidebar.metric("Balance", f"${wallet_balance:.2f}")

with st.sidebar.expander("Top-up Wallet"):
    topup_amt = st.number_input("Amount ($)", min_value=5.0, step=5.0)
    if st.button("Add Funds"):
        res = requests.post(f"{BASE_URL}/consumer/{CONSUMER_ID}/wallet/topup", params={"amount": topup_amt})
        if res.status_code == 200:
            st.success("Successfully added funds!")
            st.rerun()
        else:
            st.error("Top-up failed.")

if menu == "Marketplace & Community Cart":
    st.subheader("Available Produce & Community Pools")
    
    # 1. Show existing active pools to join
    pools = get_active_pools()
    if pools:
        st.info(f"🎉 Found **{len(pools)}** active Community Pool(s)! Join one to help hit the MOQ threshold.")
        for pool in pools:
            batch = pool.get('harvestBatch', {})
            produce_name = batch.get('produceType', 'Unknown Produce')
            pool_id = pool.get('orderId', '?')
            target = pool.get('targetMinimumOrder', 1)
            current = pool.get('poolTotalQuantity', 0)
            progress = current / target if target > 0 else 0
            
            with st.expander(f"🛒 Pool #{pool_id}: {produce_name} — {current:.0f}/{target:.0f} kg ({progress*100:.0f}% filled)"):
                col1, col2 = st.columns([3, 1])
                with col1:
                    st.write(f"**Target MOQ:** {target} kg | **Current Pool:** {current} kg")
                    st.progress(min(progress, 1.0))
                    if batch:
                        st.caption(f"Price: ${batch.get('basePrice', 0):.2f}/kg | Batch #{batch.get('batchId', '?')}")
                with col2:
                    qty = st.number_input("Qty (kg)", min_value=1.0, step=1.0, key=f"pool_qty_{pool_id}")
                    if st.button("Join Pool", key=f"pool_btn_{pool_id}"):
                        res = requests.post(f"{BASE_URL}/consumer/{CONSUMER_ID}/orders/{pool_id}/join", params={"quantity": qty})
                        if res.status_code == 200:
                            st.success("Successfully joined the pool!")
                            st.rerun()
                        else:
                            st.error(f"Failed to join: {res.text}")
    else:
        st.info("No active community pools right now. Start one from the produce list below!")
    
    st.divider()
    
    # 2. Show all available produce (to start a new pool)
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
                    st.metric("Total Available", f"{item['totalQuantity'] - item['quantitySold']} kg")
                with col3:
                    moq_target = st.number_input("Target MOQ (kg)", min_value=10.0, step=5.0, value=20.0, key=f"moq_{item['batchId']}")
                    if st.button("Start New Community Pool", key=f"start_{item['batchId']}"):
                        res = requests.post(f"{BASE_URL}/consumer/orders/start", params={"batchId": item['batchId'], "moq": moq_target})
                        if res.status_code == 200:
                            st.success(f"New pool started for {item['produceType']}!")
                            st.rerun()
                        else:
                            st.error("Failed to start pool. Check backend logs.")

elif menu == "Weekly SubscriptionBox":
    st.subheader("Manage Weekly Subscription Box")
    
    my_subs = get_my_subscriptions()
    active_sub = my_subs[0] if my_subs else None
    
    status = st.toggle("Activate Weekly Subscription", value=active_sub is not None)
    
    if status and not active_sub:
        with st.form("new_sub"):
            st.markdown("### Create New Subscription")
            box_type = st.selectbox("Box Type", ["VEGGIE", "FRUIT"])
            frequency = st.selectbox("Frequency", ["WEEKLY", "BIWEEKLY"])
            preference = st.text_input("Produce Preferences (e.g. Only local, No cilantro)")
            if st.form_submit_button("Subscribe Now"):
                res = requests.post(f"{BASE_URL}/subscription/{CONSUMER_ID}/create", 
                                    params={"boxType": box_type, "frequency": frequency, "preference": preference})
                if res.status_code == 200:
                    st.success("Subscription created successfully!")
                    st.rerun()
                else:
                    st.error(f"Failed to create subscription: {res.text}")
    
    elif status and active_sub:
        st.success(f"Your **{active_sub.get('boxType')}** subscription is ACTIVE.")
        st.write(f"**Frequency:** {active_sub.get('frequency')}")
        
        # Fixed typo: pricPerCycle -> pricePerCycle
        sub_price = active_sub.get('pricePerCycle', active_sub.get('priceCycle', 0))
        st.write(f"**Price per cycle:** ${sub_price:.2f}")
        
        pref = active_sub.get('veggiePreference') or active_sub.get('fruitPreference') or 'None set'
        st.write(f"**Preferences:** {pref}")
        
        # Check wallet balance adequacy for subscriptions
        if wallet_balance < sub_price:
            st.error(f"⚠️ Insufficient Wallet Balance (${wallet_balance:.2f}) for next renewal (${sub_price:.2f}).")
            if st.button("Send Email: Insufficient Wallet Balance (Mock)", help="Triggers backend to mock an email notification"):
                notify_res = requests.post(f"{BASE_URL}/consumer/{CONSUMER_ID}/wallet/notify-insufficient")
                if notify_res.status_code == 200:
                    st.success("Email notification sent successfully!")
                else:
                    st.error("Failed to send notification.")
        
        if st.button("Cancel Subscription", type="secondary"):
            res = requests.delete(f"{BASE_URL}/subscription/{active_sub.get('subscriptionId')}")
            if res.status_code == 200:
                st.warning("Subscription cancelled.")
                st.rerun()
    else:
        st.info("Subscribe to get fresh seasonal boxes delivered to your door.")

elif menu == "Checkout & Slots":
    st.subheader("Checkout & Delivery Booking")
    
    # 1. Fetch joined orders
    my_orders = get_my_orders()
    
    if not my_orders:
        st.warning("You haven't joined any community pools yet. Go to the Marketplace to join one!")
    else:
        st.markdown("### 1. Select Your Order")
        order_options = {}
        for o in my_orders:
            batch = o.get('harvestBatch', {})
            produce_name = batch.get('produceType', 'Unknown') if batch else 'Unknown'
            label = f"Order #{o['orderId']} - {produce_name} (Status: {o['status']})"
            order_options[label] = o
        
        selected_order_label = st.selectbox("Your Active Orders", options=list(order_options.keys()))
        selected_order = order_options[selected_order_label]
        
        # Calculate Amount
        batch = selected_order.get('harvestBatch', {})
        price_per_kg = batch.get('basePrice', 0) if batch else 0
        total_qty = selected_order.get('poolTotalQuantity', 0)
        estimated_amount = price_per_kg * total_qty
        
        st.info(f"Estimated Total for this Community Pool: **${estimated_amount:.2f}**")
        
        st.markdown("### 2. Select a Delivery Slot")
        available_slots = get_logistics_slots()
        
        if not available_slots:
            st.error("No delivery slots available currently.")
        else:
            slot_options = {}
            for s in available_slots:
                remaining = s['maxCapacity'] - s.get('currentBookings', s.get('current_bookings', 0))
                label = f"{s['slotTime']} - {s['zone']} ({remaining} spots left)"
                slot_options[label] = s['slotId']
            
            selected_slot_label = st.selectbox("Available Logistics Slots", options=list(slot_options.keys()))
            selected_slot_id = slot_options[selected_slot_label]
            
            st.divider()
            st.markdown("### 3. Payment Method")
            pay_method = st.radio("Payment Type", ["Wallet", "UPI", "Credit Card"], horizontal=True)
            if pay_method == "Wallet":
                st.caption(f"Current Wallet Balance: **${wallet_balance:.2f}**")
                if wallet_balance < estimated_amount:
                    st.error("Insufficient wallet balance for this order. Please top-up via sidebar.")
            
            
            if st.button("Confirm Order & Book Delivery", type="primary"):
                payload = {
                    "orderId": selected_order['orderId'],
                    "slotId": selected_slot_id,
                    "consumerId": CONSUMER_ID,
                    "amount": estimated_amount,
                    "paymentMethod": pay_method
                }
                
                res = requests.post(f"{BASE_URL}/consumer/checkout", json=payload)
                if res.status_code == 200:
                    st.balloons()
                    st.success(f"Payment Successful! Delivery slot booked.")
                    st.markdown("Check your order status in the Marketplace (it should now be **FULFILLED**).")
                else:
                    st.error(f"Checkout Failed: {res.text}")
