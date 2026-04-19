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
    
    # 2. Show all available produce (to start a new pool - logic for starting new pools can be added later)
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
                    # IMPLEMENTED: Open a new GroupOrder
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
    
    st.markdown("### Select a Delivery Slot")
    available_slots = get_logistics_slots()
    
    if not available_slots:
        st.warning("No delivery slots available for your zone currently.")
    else:
        slot_options = {f"{s['slotTime']} - {s['zone']} ({s['maxCapacity'] - s['current_bookings']} left)": s['slotId'] for s in available_slots}
        selected_label = st.selectbox("Available Logistics Slots", options=list(slot_options.keys()))
        selected_slot_id = slot_options[selected_label]
        
        st.divider()
        st.info("Note: Checkout requires an active Order ID. Complete a 'Join Pool' action first.")
        
        # This is a sample button - in real app, we'd tether to a specific confirmed GroupOrder
        if st.button("Complete Checkout", type="primary"):
            st.info("Checkout process requires a confirmed pool. Join an active pool in the Market tab first!")
