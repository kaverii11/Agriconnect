import streamlit as st
from PIL import Image
import base64

# Set up page configurations
st.set_page_config(
    page_title="AgriConnect",
    page_icon="🌾",
    layout="wide",
    initial_sidebar_state="expanded",
)

def main():
    # Load and display logo
    try:
        # Read image to base64 to ensure 100% accurate HTML-based centering
        with open('assets/logo.png', "rb") as image_file:
            encoded_string = base64.b64encode(image_file.read()).decode()
            
        st.markdown(
            f"""
            <div style="display: flex; justify-content: center; align-items: center; width: 100%;">
                <img src="data:image/png;base64,{encoded_string}" style="width: 25%; max-width: 250px; height: auto;">
            </div>
            """, 
            unsafe_allow_html=True
        )
    except FileNotFoundError:
        st.warning("Logo not found in assets/logo.png. Ensure the image is present.")

    st.markdown("<h1 style='text-align: center; color: #2E7D32;'>Welcome to AgriConnect</h1>", unsafe_allow_html=True)
    st.markdown("<h4 style='text-align: center; color: #3E2723;'>Farm-to-Table Community Commerce Platform</h4>", unsafe_allow_html=True)
    
    st.divider()

    st.markdown("""
    ### 🌱 What is AgriConnect?
    AgriConnect is a digital marketplace bridging the gap between local farmers and community consumers. 
    By leveraging direct sales and aggregated community orders, we minimize wastage, guarantee fresh produce, and ensure fair prices for farmers.

    ---
    ### 🧭 Navigate using the sidebar:
    - **🌾 Farmer View:** Manage harvest batches, view sales, and dynamically set your prices.
    - **🛒 Consumer View:** Browse fresh produce, join community carts, and manage weekly subscriptions.
    - **📈 Admin View:** System oversight, user verification, and overall analytics.
    """)

if __name__ == "__main__":
    main()
