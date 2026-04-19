import streamlit as st
import pandas as pd
import plotly.express as px
import datetime

st.set_page_config(page_title="Farmer Dashboard", page_icon="🌾", layout="wide")

st.title("🌾 Farmer Dashboard")
st.markdown("Manage your harvest batches, adjust pricing, and view analytics.")

# Mock API functions (Replace these with requests to your Java Spring Boot backend)
def mock_get_active_batches():
    return pd.DataFrame({
        "Batch ID": [101, 102, 103],
        "Produce": ["Tomatoes", "Carrots", "Potatoes"],
        "Base Price (per kg)": [2.50, 1.20, 0.80],
        "Total Qty (kg)": [500, 300, 1000],
        "Qty Sold (kg)": [150, 280, 400],
        "Harvest Date": ["2026-04-10", "2026-04-12", "2026-04-15"]
    })

def mock_get_sales_metrics():
    return {"total_sales": 1450.50, "upcoming_orders": 12, "wastage_saved": 85}

st.sidebar.header("Welcome, Farmer John!")
view_selection = st.sidebar.radio("Navigation", ["Overview", "Add Harvest Batch"])

if view_selection == "Overview":
    # Metrics
    metrics = mock_get_sales_metrics()
    col1, col2, col3 = st.columns(3)
    col1.metric("Total Sales ($)", f"${metrics['total_sales']:.2f}")
    col2.metric("Upcoming Orders", metrics['upcoming_orders'])
    col3.metric("Wastage Avoided (kg)", f"{metrics['wastage_saved']} kg")
    
    st.divider()
    
    # Active Batches Table
    st.subheader("Your Active Harvest Batches")
    df_batches = mock_get_active_batches()
    
    # Visualizing stock
    df_batches["Stock Remaining"] = df_batches["Total Qty (kg)"] - df_batches["Qty Sold (kg)"]
    st.dataframe(df_batches, use_container_width=True, hide_index=True)
    
    # Simple Chart
    st.subheader("Sales Volume vs Remaining Stock")
    fig = px.bar(df_batches, x="Produce", y=["Qty Sold (kg)", "Stock Remaining"], 
                 title="Produce Stock Breakdown",
                 color_discrete_sequence=["#81C784", "#388E3C"])
    st.plotly_chart(fig, use_container_width=True)
    
    # Dynamic Pricing form mock
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
            st.success(f"Successfully updated pricing for Batch {selected_batch} via mock API PUT request!")

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
                # Mock POST request
                st.success(f"Batch for {qty}kg of {produce_type} added successfully! (Mock POST)")
                st.info("In production, this translates to: `requests.post('/api/farmers/batches', json={...})`")
            else:
                st.error("Please enter a produce type.")
