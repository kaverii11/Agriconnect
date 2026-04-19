#!/bin/bash

# ========================================================
# AgriConnect - MySQL Database Setup Script
# ========================================================

DB_NAME="agriconnect_db"
NEW_PASS="Prakul27#"

echo "----------------------------------------------------"
echo "AgriConnect Database Setup"
echo "----------------------------------------------------"

# Step 1: Attempt to create the database with the new password
echo "[1/2] Creating database '$DB_NAME'..."
mysql -u root -p"$NEW_PASS" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ Database created successfully (or already exists)."
else
    echo "❌ Failed to connect with '$NEW_PASS'."
    echo "    Attempting passwordless connection..."
    mysql -u root -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo "✅ Database created (using no password)."
        echo "    Updating root password to '$NEW_PASS'..."
        mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$NEW_PASS'; FLUSH PRIVILEGES;"
        echo "✅ Password updated to '$NEW_PASS'."
    else
        echo "❌ Access Denied. Please run the following command manually to reset your password:"
        echo ""
        echo "    sudo mysql -e \"ALTER USER 'root'@'localhost' IDENTIFIED BY 'AgriConnect#123'; FLUSH PRIVILEGES;\""
        echo ""
        exit 1
    fi
fi

echo "[2/2] Setup complete."
echo "Your database name is: $DB_NAME"
echo "Your root password is: $NEW_PASS"
echo "----------------------------------------------------"
