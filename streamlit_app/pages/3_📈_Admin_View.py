import streamlit as st
import pandas as pd
import plotly.express as px

st.set_page_config(page_title="Admin Dashboard", page_icon="📈", layout="wide")

st.title("📈 Admin System Dashboard")
st.markdown("System oversight, user management, and platform analytics.")

# --- Mock Data ---
def mock_get_users():
    return pd.DataFrame({
        "User ID": [2, 3, 4, 5, 6],
        "Name": ["Farmer John Doe", "Farmer Sarah Smith", "Alice Consumer", "Bob Brown", "Charlie Clark"],
        "Role": ["FARMER", "FARMER", "CONSUMER", "CONSUMER", "CONSUMER"],
        "Email": ["john@farmers.com", "sarah@rural.com", "alice@gmail.com", "bob@yahoo.com", "charlie@outlook.com"],
        "Status": ["Verified", "Pending", "Active", "Active", "Active"]
    })

def mock_get_system_metrics():
    return {
        "total_revenue": 125000.75,
        "active_subs": 350,
        "wastage_percent": 4.2
    }

# --- Sidebar ---
st.sidebar.header("Welcome, Admin!")
menu = st.sidebar.radio("Navigation", ["Analytics Dashboard", "User Verification"])

if menu == "Analytics Dashboard":
    metrics = mock_get_system_metrics()
    
    # Top KPI Cards
    col1, col2, col3 = st.columns(3)
    col1.metric("Platform Revenue (YTD)", f"${metrics['total_revenue']:,.2f}", "+15%")
    col2.metric("Active Subscriptions", metrics['active_subs'], "+12")
    col3.metric("Platform Wastage %", f"{metrics['wastage_percent']}%", "-1.5%", delta_color="inverse")
    
    st.divider()
    
    # Mocking a timeseries chart for revenue
    st.subheader("Revenue Growth over Time")
    dates = pd.date_range(start="2026-01-01", periods=4, freq="MS")
    rev_data = pd.DataFrame({
        "Month": dates,
        "Revenue": [25000, 28000, 32000, 40000]
    })
    fig = px.line(rev_data, x="Month", y="Revenue", markers=True, title="Monthly System Revenue ($)")
    fig.update_traces(line_color="#2E7D32")
    st.plotly_chart(fig, use_container_width=True)

elif menu == "User Verification":
    st.subheader("Manage Users & Verifications")
    
    df_users = mock_get_users()
    st.dataframe(df_users, use_container_width=True, hide_index=True)
    
    st.markdown("### Verify Farmer Account")
    colA, colB = st.columns([3, 1])
    with colA:
        farmer_to_verify = st.selectbox("Select Pending Farmer", df_users[df_users["Status"] == "Pending"]["Name"])
    with colB:
        st.write("") # spacing
        st.write("") # spacing
        if st.button("Approve Farmer Verification"):
            # Mock API Put request
            st.success(f"Status for {farmer_to_verify} updated from Pending to Verified! (Mock PUT via API)")

