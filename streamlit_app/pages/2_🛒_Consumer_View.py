import streamlit as st
import pandas as pd

st.set_page_config(page_title="Consumer Marketplace", page_icon="🛒", layout="wide")

st.title("🛒 Consumer Marketplace")
st.markdown("Browse fresh produce, join community group orders, and manage subscriptions.")

# --- Mock Data ---
def mock_get_marketplace_items():
    return [
        {"id": 1, "farm": "Green Valley Farm", "produce": "Tomatoes", "price": 2.50, "moq": 50, "current_pool": 35},
        {"id": 2, "farm": "Sunny Pastures", "produce": "Carrots", "price": 1.10, "moq": 100, "current_pool": 90},
        {"id": 3, "farm": "Green Valley Farm", "produce": "Potatoes", "price": 0.80, "moq": 200, "current_pool": 50},
    ]

# --- Sidebar ---
st.sidebar.header("Welcome, Alice!")
menu = st.sidebar.radio("Navigation", ["Marketplace & Community Cart", "Weekly SubscriptionBox", "Checkout & Slots"])

if menu == "Marketplace & Community Cart":
    st.subheader("Available Produce")
    st.info("Join a GroupOrder to hit the Minimum Order Quantity (MOQ) and unlock wholesale prices!")
    
    items = mock_get_marketplace_items()
    
    for item in items:
        with st.container():
            col1, col2, col3, col4 = st.columns([2, 1, 3, 1])
            with col1:
                st.markdown(f"**{item['produce']}**")
                st.caption(f"from {item['farm']}")
                st.markdown(f"${item['price']:.2f} / kg")
            with col2:
                st.metric("MOQ Goal", f"{item['moq']} kg")
            with col3:
                progress = item['current_pool'] / item['moq']
                st.progress(min(progress, 1.0))
                st.caption(f"Current Pool: {item['current_pool']} kg")
            with col4:
                qty_to_add = st.number_input(f"Qty", min_value=1, max_value=100, key=f"qty_{item['id']}")
                if st.button("Add to Cart", key=f"btn_{item['id']}"):
                    st.success(f"Added {qty_to_add}kg of {item['produce']} to Community Cart! (Mock POST)")

elif menu == "Weekly SubscriptionBox":
    st.subheader("Manage Weekly Subscription Box")
    st.markdown("Get a curated box of fresh, seasonal produce automatically delivered!")
    
    # Mock Subscription state
    status = st.toggle("Activate Weekly Subscription", value=True)
    
    if status:
        st.success("Your subscription is ACTIVE.")
        st.markdown("**Next Delivery:** Friday, May 5th")
        
        with st.form("sub_preferences"):
            st.selectbox("Box Size", ["Small (1-2 people) - $20", "Medium (3-4 people) - $35", "Large (5+ people) - $50"], index=1)
            st.multiselect("Produce Preferences", ["Tomatoes", "Carrots", "Leafy Greens", "Apples", "Berries"], default=["Tomatoes", "Leafy Greens"])
            st.text_area("Exclusions / Allergies", placeholder="E.g., No cilantro")
            
            if st.form_submit_button("Update Preferences"):
                st.success("Preferences updated via API!")
    else:
        st.warning("Your subscription is completely inactive.")

elif menu == "Checkout & Slots":
    st.subheader("Checkout & Delivery Booking")
    
    st.markdown("### Your Cart Total: **$14.50**")
    
    st.markdown("### Select a Delivery Slot")
    # Mock slots
    slots = ["2026-05-01 09:00 AM - City Center", "2026-05-01 02:00 PM - Suburbs", "2026-05-02 10:00 AM - North Hills"]
    selected_slot = st.selectbox("Available Logistics Slots", slots)
    
    st.divider()
    if st.button("Confirm Order & Book Slot", type="primary"):
        st.success(f"Order confirmed! Booked slot: {selected_slot}. (Mock POST to Payment & Logistics API)")
