<h2 align="center">

<h2 align="center">
  <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
  🎓  FACULTY OF INFORMATION TECHNOLOGY (DAINAM UNIVERSITY)
  </a>
</h2>
<h2 align="center">
  🔐 HỆ THỐNG ĐĂNG NHẬP CLIENT-SERVER
</h2>

<div align="center">
    <p align="center">
        <img alt="aiotlab_logo" src="https://github.com/user-attachments/assets/d160de9e-7aa4-47f0-9a65-c6275a736d58" width="170" />
        <img alt="fitdnu_logo" src="https://github.com/user-attachments/assets/f40bd9aa-d77b-434a-91aa-7e83e41b90ff" width="180"/>
        <img alt="dnu_logo" src="https://github.com/user-attachments/assets/4e6392f6-664e-46d8-b411-5d58b257d721" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

---

## 1. Giới thiệu hệ thống 📋

Hệ thống quản lý đăng nhập người dùng cung cấp các chức năng đăng nhập, quản lý tài khoản, phân quyền, ghi nhận lịch sử truy cập và bảo mật tài khoản. Mục tiêu của hệ thống là xây dựng một giải pháp quản lý người dùng an toàn, dễ sử dụng, phù hợp với các ứng dụng doanh nghiệp vừa và nhỏ.

### Các chức năng chính ⚡

- Đăng nhập, đăng xuất
- Quản lý tài khoản người dùng (tạo, sửa, khóa/mở khóa)
- User có thể thay đổi thông tin cá nhân và thay đổi mật khẩu
- Phân quyền người dùng (ADMIN/USER)
- Ghi nhận lịch sử đăng nhập, hành động
- Bảo vệ tài khoản với chính sách khóa khi đăng nhập sai nhiều lần
- Quản lý trạng thái online/offline
- Kiểm tra trạng thái tài khoản: Hiển thị trạng thái hoạt động của tài khoản (online/offline, bị khóa,...).
- Thống kê và báo cáo: Hiển thị biểu đồ thống kê hoạt động người dùng.
- Tìm kiếm nâng cao: Lọc và tìm kiếm tài khoản theo nhiều tiêu chí (tên, email, trạng thái,...).

### Mục tiêu hệ thống 🎯

- Đảm bảo an toàn thông tin người dùng
- Quản lý tập trung, dễ dàng mở rộng
- Giao diện thân thiện, hiện đại
- Hỗ trợ kiểm tra, giám sát hoạt động truy cập

## 2. Các công nghệ được sử dụng 🛠️

- **Ngôn ngữ:** Java
- **Giao diện:** Java Swing
- **Giao thức mạng:** TCP Socket
- **Lưu trữ:** MySQL Database
- **Môi trường phát triển:** Eclipse IDE
- **Hệ điều hành:** Windows

## 3. Giao diện 🖥️

- Giao diện hiện đại, sử dụng màu sắc và font chữ thân thiện
- Hỗ trợ đăng nhập, quản lý tài khoản, xem lịch sử truy cập
- Quản trị viên có thể thao tác trực tiếp trên bảng người dùng
- Ảnh minh họa giao diện:

<div align="center">
<table>
  <tr>
      <td align="center">
      <img width="100%"alt="Screenshot 2025-09-19 080953" src="https://github.com/user-attachments/assets/2f1f58c5-6bdc-4fc5-8731-f23a270a01e2" /><br/>
      <b>Giao diện đăng nhập</b>
    </td>
  </tr>
</table>

</div>

<div align="center">

<table>
  <tr>
    <td align="center">
      <img width="110%" alt="Screenshot 2025-09-19 055603" src="https://github.com/user-attachments/assets/ff5bdebe-ae2c-4dd6-9a30-a4dcac8db26c" /><br/>
      <b>Giao diện đăng ký</b>
    </td>
        <td align="center">
      <img width="70%" alt="Screenshot 2025-09-19 060423" src="https://github.com/user-attachments/assets/1789505a-e262-4e7f-a87e-50ba1141a3bf" /><br/>
      <b>Giao diện User</b>
    </td>
  </tr>
</table>

</div>
<div >
<table>
  <tr>
    <td align="center">
      <img width="90%" alt="Screenshot 2025-09-19 060705" src="https://github.com/user-attachments/assets/46fcfa65-4510-47d3-814a-2a2d63a0cdac" /><br/>
      <b>Giao diện sửa thông tin của User</b>
    </td>
      <td align="center">
      <img width="90%"alt="Screenshot 2025-09-19 060900" src="https://github.com/user-attachments/assets/9254a83d-c3c4-47cd-901f-108c89b07013" /><br/>
      <b>Giao diện đổi mật khẩu của User</b>
    </td>
  </tr>
</table>

<div align="center">
<table>
  <tr>
      <td align="center">
      <img width="100%" alt="Screenshot 2025-09-19 061113" src="https://github.com/user-attachments/assets/537203c7-8b17-48be-9038-8ae175035a27" /><br/>
      <b>Giao diện quản lý của Admin</b>
    </td>
  </tr>
</table>

</div>

<div align="center">
<table>
  <tr>
       <td align="center">
      <img width="100%" alt="Screenshot 2025-09-19 072421" src="https://github.com/user-attachments/assets/204fd50c-1998-4041-8aae-29c0122930bb" /><br/>
      <b>Giao diện khóa/ mở khóa tài khoản</b>
  </tr>
</table>

</div>
<div align="center">
<table>
  <tr>
       <td align="center">
      <img width="100%"alt="Screenshot 2025-09-19 072628" src="https://github.com/user-attachments/assets/1458fbc2-a596-41d4-9162-91983ced8b38" /><br/>
      <b>Giao diện lịch sử tài khoản</b>
    </td>
  </tr>
</table>

</div>

<div align="center">
<table>
  <tr>
      <td align="center">
      <img width="100%" alt="Screenshot 2025-09-19 072228" src="https://github.com/user-attachments/assets/9b5d12c1-dacc-47b0-bc1c-98bd3590d3db" /><br/>
      <b>Giao diện thêm người dùng của Admin</b>
    </td>
  </tr>
</table>

</div>

## 4. Các bước cài đặt & sử dụng 🚀

### 4.1. Chuẩn bị môi trường 🏗️

- **Cài đặt JDK**: Phiên bản 11 hoặc cao hơn (khuyến nghị JDK 17).
- **Cài đặt Eclipse IDE**: Dùng để import và build project.
- **Cài đặt MySQL Server**: Phiên bản 5.7 hoặc 8.x.
- **Tạo cơ sở dữ liệu**: Dùng MySQL Workbench hoặc dòng lệnh.

### 4.2. Tạo cơ sở dữ liệu 🗄️

Chạy các câu lệnh SQL sau để tạo database và các bảng cần thiết:

```sql
-- Tạo database
CREATE DATABASE IF NOT EXISTS user_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Sử dụng database
USE user_management;

-- Tạo bảng users
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(150),
  email VARCHAR(150),
  role ENUM('USER','ADMIN') DEFAULT 'USER',
  status ENUM('ACTIVE','LOCKED','INACTIVE') DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  last_login TIMESTAMP NULL
);

-- Tạo bảng login_attempts
CREATE TABLE login_attempts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  username VARCHAR(50),
  attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  success BOOLEAN,
  ip VARCHAR(45),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Tạo bảng audit_logs
CREATE TABLE audit_logs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  action VARCHAR(100),
  details TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

📌 Sau khi chạy xong, bạn sẽ có đầy đủ cấu trúc database để hệ thống hoạt động.

### 4.3. Cấu hình kết nối CSDL 🔗

Mở file `com.myapp.server.MySQLDatabase.java` và chỉnh sửa thông tin kết nối:

```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/user_management";
private static final String DB_USER = "root";       // thay bằng user MySQL của bạn
private static final String DB_PASSWORD = "123456"; // thay bằng password của bạn
```

### 4.4. Biên dịch và chạy dự án ⚙️

Mở Eclipse IDE → File > Import > Existing Projects into Workspace.
Chọn thư mục project (LoginSystem).
Chuột phải project → Build Project.

### 4.5. Khởi chạy hệ thống ▶️

Khởi động Server: chạy file `ServerMain.java` (ở package `com.myapp.server`).
Khởi động Client: chạy file `ClientMain.java` (ở package `com.myapp.client`).

### 4.6. Đăng nhập & sử dụng 👤

Tài khoản mặc định:

- Username: `admin`
- Password: `admin123`

User có thể:

- Đăng nhập để xem thông tin cá nhân.
- Sửa thông tin cá nhân.
- Đổi mật khẩu.

Admin có thể:

- Quản lý tài khoản người dùng (thêm, sửa, khóa/mở khóa).
- Phân quyền (USER / ADMIN).
- Xem nhật ký hoạt động (audit logs).
- Lọc và tìm kiếm danh sách tài khoản.
- Xem thống kê và báo cáo hoạt động người dùng.
- Kiểm tra trạng thái tài khoản chi tiết.
- Kiểm tra trạng thái tài khoản (online/offline).

## 5. Liên hệ 📞

- **Họ tên:** Nguyễn Thanh Bình
- **Email:** nguyenbinh041104@gmail.com
- **SĐT:** 0839705780
