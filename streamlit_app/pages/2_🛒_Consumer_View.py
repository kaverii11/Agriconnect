import streamlit as st
import pandas as pd
import requests

st.set_page_config(page_title="Consumer Marketplace", page_icon="🛒", layout="wide")

st.title("🛒 Consumer Marketplace")
st.markdown("Browse fresh produce, join community group orders, and manage subscriptions.")

# --- Constants ---
BASE_URL = "http://localhost:8080/api"
CONSUMER_ID = 4  # Alice Consumer from data.sql

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
        response = requests.get(f"{BASE_URL}/{CONSUMER_ID}/my-orders") # Note: BASE_URL/consumer prefix handled locally
        # Wait, my Base URL is http://localhost:8080/api
        # My endpoint is /api/consumer/{consumerId}/my-orders
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

# --- Sidebar ---
st.sidebar.header("Welcome, Alice!")
menu = st.sidebar.radio("Navigation", ["Marketplace & Community Cart", "Weekly SubscriptionBox", "Checkout & Slots"])

if menu == "Marketplace & Community Cart":
    st.subheader("Available Produce & Community Pools")
    
    # 1. Show existing active pools to join
    pools = get_active_pools()
    if pools:
        st.info("Found active Community Carts! Join one to help hit the MOQ threshold.")
        for pool in pools:
            batch = pool.get('harvestBatch', {})
            with st.expander(f"🛒 Join Pool: {batch.get('produceType')} (ID #{pool.get('orderId')})"):
                col1, col2 = st.columns([3, 1])
                with col1:
                    st.write(f"**Target MOQ:** {pool.get('targetMinimumOrder')} kg | **Current Pool:** {pool.get('poolTotalQuantity')} kg")
                    progress = pool.get('poolTotalQuantity', 0) / pool.get('targetMinimumOrder', 1)
                    st.progress(min(progress, 1.0))
                with col2:
                    qty = st.number_input("Qty (kg)", min_value=1.0, step=1.0, key=f"pool_qty_{pool.get('orderId')}")
                    if st.button("Join Pool", key=f"pool_btn_{pool.get('orderId')}"):
                        res = requests.post(f"{BASE_URL}/consumer/{CONSUMER_ID}/orders/{pool.get('orderId')}/join", params={"quantity": qty})
                        if res.status_code == 200:
                            st.success("Successfully joined the pool!")
                            st.rerun()
    
    st.divider()
    
    # 2. Show all available produce
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
                    # Open a new GroupOrder
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
    
    elif status and active_sub:
        st.success(f"Your {active_sub.get('boxType')} subscription is ACTIVE.")
        st.write(f"**Frequency:** {active_sub.get('frequency')}")
        st.write(f"**Preferences:** {active_sub.get('veggiePreference') or active_sub.get('fruitPreference')}")
        
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
        # Filter for OPEN or CONFIRMED orders (only those are checkoutable in this flow)
        order_options = {f"Order #{o['orderId']} - {o['harvestBatch']['produceType']} (Status: {o['status']})": o for o in my_orders}
        selected_order_label = st.selectbox("Your Active Orders", options=list(order_options.keys()))
        selected_order = order_options[selected_order_label]
        
        # Calculate Amount (Price * total pool shared? No, in simplified model we charge a flat estimated amount or total bill)
        # Let's say we charge: batch basePrice * poolTotalQuantity / size (split bill)
        # For simplicity, we'll just show the total batch price for the shared quantities
        price_per_kg = selected_order['harvestBatch']['basePrice']
        total_qty = selected_order['poolTotalQuantity']
        estimated_amount = price_per_kg * total_qty
        
        st.info(f"Estimated Total for this Community Pool: **${estimated_amount:.2f}**")
        
        st.markdown("### 2. Select a Delivery Slot")
        available_slots = get_logistics_slots()
        
        if not available_slots:
            st.error("No delivery slots available for your zone currently.")
        else:
            slot_options = {f"{s['slotTime']} - {s['zone']} ({s['maxCapacity'] - s['current_bookings']} left)": s['slotId'] for s in available_slots}
            selected_slot_label = st.selectbox("Available Logistics Slots", options=list(slot_options.keys()))
            selected_slot_id = slot_options[selected_label] if 'selected_label' in locals() else list(slot_options.values())[0]
            # Wait, fixed the name issue
            selected_slot_id = slot_options[selected_slot_label]
            
            st.divider()
            st.markdown("### 3. Payment Method")
            pay_method = st.radio("Payment Type", ["UPI", "Credit Card", "Wallet"], horizontal=True)
            
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
                    st.success(f"Payment Successful! Delivery slot booked for {selected_slot_label}.")
                    st.markdown("Check your order status in the Marketplace (it should now be **FULFILLED**).")
                else:
                    st.error(f"Checkout Failed: {res.text}")
