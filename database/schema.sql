IF DB_ID(N'SHOEStoremanaj') IS NULL
    CREATE DATABASE SHOEStoremanaj;
GO

USE SHOEStoremanaj;
GO

IF OBJECT_ID(N'dbo.customer', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.customer (
        customer_id NVARCHAR(50) NOT NULL PRIMARY KEY,
        customer_name NVARCHAR(100) NOT NULL,
        customer_address NVARCHAR(255) NOT NULL,
        phone_number NVARCHAR(30) NOT NULL
    );
END;
GO

IF OBJECT_ID(N'dbo.employee', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.employee (
        employee_id NVARCHAR(50) NOT NULL PRIMARY KEY,
        employee_name NVARCHAR(100) NOT NULL,
        employee_address NVARCHAR(255) NOT NULL,
        phone_number NVARCHAR(30) NOT NULL
    );
END;
GO

IF OBJECT_ID(N'dbo.supplier', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.supplier (
        supplier_id NVARCHAR(50) NOT NULL PRIMARY KEY,
        supplier_name NVARCHAR(100) NOT NULL,
        supplier_address NVARCHAR(255) NOT NULL,
        supplier_brand NVARCHAR(100) NOT NULL,
        phone_number NVARCHAR(30) NOT NULL
    );
END;
GO

IF OBJECT_ID(N'dbo.product', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.product (
        shoe_id NVARCHAR(50) NOT NULL PRIMARY KEY,
        shoe_brand NVARCHAR(100) NOT NULL,
        shoe_name NVARCHAR(100) NOT NULL,
        color NVARCHAR(50) NOT NULL,
        size DECIMAL(5, 2) NOT NULL CHECK (size > 0),
        price DECIMAL(18, 2) NOT NULL CHECK (price >= 0),
        stock INT NOT NULL CHECK (stock >= 0)
    );
END;
GO

-- Akun dibuat melalui AccountTool agar kolom password berisi hash PBKDF2.
-- Tabel lama tidak diubah otomatis; AccountTool migrate bekerja pada satu akun.
IF OBJECT_ID(N'dbo.login', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.login (
        username NVARCHAR(100) NOT NULL PRIMARY KEY,
        password NVARCHAR(255) NOT NULL
    );
END;
GO
