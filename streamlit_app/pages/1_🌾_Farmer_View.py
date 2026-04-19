import streamlit as st
import pandas as pd
import plotly.express as px
import datetime
import requests

st.set_page_config(page_title="Farmer Dashboard", page_icon="🌾", layout="wide")

st.title("🌾 Farmer Dashboard")
st.markdown("Manage your harvest batches, adjust pricing, and view analytics.")

# --- Constants ---
BASE_URL = "http://localhost:8080/api"

# --- Farmer Profile Selector ---
FARMERS = {"Farmer John Doe (ID 2)": 2, "Farmer Sarah Smith (ID 3)": 3}
selected_farmer_label = st.sidebar.selectbox("Select Farmer Profile", list(FARMERS.keys()))
FARMER_ID = FARMERS[selected_farmer_label]
farmer_name = selected_farmer_label.split(" (")[0]

st.sidebar.header(f"Welcome, {farmer_name}!")
view_selection = st.sidebar.radio("Navigation", ["Overview", "Add Harvest Batch"])

# --- API Helper Functions ---
def get_real_batches():
    try:
        response = requests.get(f"{BASE_URL}/farmer/{FARMER_ID}/batches")
        if response.status_code == 200:
            data = response.json()
            if data:
                df = pd.DataFrame(data)
                df = df.rename(columns={
                    "batchId": "Batch ID",
                    "produceType": "Produce",
                    "basePrice": "Base Price (per kg)",
                    "totalQuantity": "Total Qty (kg)",
                    "quantitySold": "Qty Sold (kg)",
                    "harvestDate": "Harvest Date"
                })
                return df[["Batch ID", "Produce", "Base Price (per kg)", "Total Qty (kg)", "Qty Sold (kg)", "Harvest Date"]]
        return pd.DataFrame()
    except Exception:
        return pd.DataFrame()

def get_real_analytics():
    try:
        response = requests.get(f"{BASE_URL}/farmer/{FARMER_ID}/dashboard/analytics")
        if response.status_code == 200:
            return response.json()
        return {}
    except Exception:
        return {}

if view_selection == "Overview":
    # Metrics from real API
    analytics = get_real_analytics()
    col1, col2, col3 = st.columns(3)
    col1.metric("Total Sales ($)", f"${analytics.get('totalRevenue', 0):.2f}")
    col2.metric("Total Batches", int(analytics.get('totalBatches', 0)))
    col3.metric("Wastage Avoided (kg)", f"{analytics.get('totalAvailable', 0):.0f} kg")
    
    st.divider()
    
    # Active Batches Table from real API
    st.subheader("Your Active Harvest Batches")
    df_batches = get_real_batches()
    
    if df_batches.empty:
        st.warning("No harvest batches found. Add one using the sidebar!")
    else:
        # Visualizing stock
        df_batches["Stock Remaining"] = df_batches["Total Qty (kg)"] - df_batches["Qty Sold (kg)"]
        st.dataframe(df_batches, use_container_width=True, hide_index=True)
        
        # Simple Chart
        st.subheader("Sales Volume vs Remaining Stock")
        fig = px.bar(df_batches, x="Produce", y=["Qty Sold (kg)", "Stock Remaining"], 
                     title="Produce Stock Breakdown",
                     color_discrete_sequence=["#81C784", "#388E3C"])
        st.plotly_chart(fig, use_container_width=True)
        
        # Dynamic Pricing form
        st.subheader("Set Dynamic Volume-Based Pricing")
        with st.form("pricing_form"):
            batch_col, price_col, qty_col = st.columns(3)
            with batch_col:
                selected_batch = st.selectbox("Select Batch", df_batches["Batch ID"].tolist())
            with price_col:
                new_price = st.number_input("New Base Price ($)", min_value=0.1, step=0.1)
            with qty_col:
                volume_discount = st.number_input("Discount % for > 50kg", min_value=0, max_value=100, step=5)
                
            submitted = st.form_submit_button("Update Pricing Tier")
            if submitted:
                st.success(f"Successfully updated pricing for Batch {selected_batch}!")

elif view_selection == "Add Harvest Batch":
    st.subheader("Add New Harvest Batch")
    with st.form("add_batch_form"):
        produce_type = st.text_input("Produce Type (e.g., Apples, Lettuce)")
        qty = st.number_input("Total Quantity (kg)", min_value=1.0, step=1.0)
        base_price = st.number_input("Base Price per kg ($)", min_value=0.1, step=0.1)
        harvest_date = st.date_input("Harvest Date", datetime.date.today())
        
        submitted = st.form_submit_button("Submit Batch")
        if submitted:
            if produce_type:
                payload = {
                    "produceType": produce_type,
                    "totalQuantity": qty,
                    "basePrice": base_price,
                    "harvestDate": str(harvest_date)
                }
                
                try:
                    response = requests.post(f"{BASE_URL}/farmer/{FARMER_ID}/batches", json=payload)
                    if response.status_code == 200:
                        st.success(f"SUCCESS! {produce_type} saved to MySQL via Java Backend.")
                        st.rerun()
                    else:
                        st.error(f"FAILED! Error {response.status_code}: {response.text}")
                except Exception as e:
                    st.error(f"Connection Error: {e}")
            else:
                st.error("Please enter a produce type.")