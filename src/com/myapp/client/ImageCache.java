package com.myapp.client;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * ImageCache - Hệ thống cache ảnh với 2 cấp độ:
 * 1. RAM Cache: Lưu ImageIcon trong HashMap để truy xuất nhanh
 * 2. Disk Cache: Lưu file ảnh vào thư mục .cache để tránh tải lại từ internet
 * 
 * Tính năng:
 * - Cache ảnh trong RAM (HashMap) để load nhanh khi sử dụng lại
 * - Cache ảnh trên disk (.cache folder) để tránh download lại
 * - Load ảnh bất đồng bộ để không block UI
 * - Tự động scale ảnh theo kích thước yêu cầu
 * - Thread-safe với ConcurrentHashMap
 */
public class ImageCache {
    // RAM Cache - lưu ImageIcon đã scale sẵn
    private static final ConcurrentHashMap<String, ImageIcon> memoryCache = new ConcurrentHashMap<>();
    
    // Thư mục cache trên disk - xác định theo thứ tự ưu tiên:
    // 1) system property 'login.cache.dir' (ví dụ: -Dlogin.cache.dir=C:\path\to\cache)
    // 2) workspace path (nếu tồn tại) -> C:\Users\Admin\eclipse-workspace\LoginSystem\.cache
    // 3) hệ thống temp (java.io.tmpdir) -> <tmp>/login_system_cache
    // 4) user.home/.cache/LoginSystem
    private static final String CACHE_DIR;
    
    // Singleton instance
    private static ImageCache instance = null;

    static {
        String chosen = System.getProperty("login.cache.dir");
        if (chosen == null || chosen.trim().isEmpty()) {
            // Thử workspace path cụ thể do người dùng cung cấp
            String workspacePath = "C:\\Users\\Admin\\eclipse-workspace\\LoginSystem";
            File ws = new File(workspacePath);
            if (ws.exists() && ws.isDirectory()) {
                chosen = workspacePath + File.separator + ".cache";
            } else {
                // Dùng tmp của hệ thống
                String tmp = System.getProperty("java.io.tmpdir");
                if (tmp != null && !tmp.trim().isEmpty()) {
                    chosen = tmp + File.separator + "login_system_cache";
                } else {
                    // Fallback cuối cùng về thư mục người dùng
                    chosen = System.getProperty("user.home") + File.separator + ".cache" + File.separator + "LoginSystem";
                }
            }
        }

        CACHE_DIR = chosen;

        // Tạo thư mục cache nếu chưa tồn tại
        try {
            Path cacheDir = Paths.get(CACHE_DIR);
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
                System.out.println("Đã tạo thư mục cache: " + CACHE_DIR);
            }
        } catch (IOException e) {
            System.err.println("Không thể tạo thư mục cache: " + e.getMessage());
        }
    }
    
    private ImageCache() {}
    
    public static ImageCache getInstance() {
        if (instance == null) {
            synchronized (ImageCache.class) {
                if (instance == null) {
                    instance = new ImageCache();
                }
            }
        }
        return instance;
    }
    
    /**
     * Load ảnh từ URL với cache và scale về kích thước mong muốn
     * @param imageUrl URL của ảnh
     * @param width chiều rộng mong muốn
     * @param height chiều cao mong muốn
     * @param callback callback được gọi khi ảnh load xong (có thể null)
     * @return ImageIcon từ cache nếu có, null nếu cần load từ internet
     */
    public ImageIcon getImage(String imageUrl, int width, int height, ImageLoadCallback callback) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        
        // Tạo cache key bao gồm URL và kích thước
        String cacheKey = imageUrl + "_" + width + "x" + height;
        
        // 1. Kiểm tra RAM cache trước
        ImageIcon cachedIcon = memoryCache.get(cacheKey);
        if (cachedIcon != null) {
            System.out.println("✓ Load ảnh từ RAM cache: " + imageUrl);
            return cachedIcon;
        }
        
        // 2. Load bất đồng bộ từ disk cache hoặc internet
        CompletableFuture.supplyAsync(() -> {
            try {
                return loadImageFromCacheOrUrl(imageUrl, width, height, cacheKey);
            } catch (Exception e) {
                System.err.println("Lỗi load ảnh từ " + imageUrl + ": " + e.getMessage());
                return null;
            }
        }).thenAccept(icon -> {
            if (icon != null && callback != null) {
                // Gọi callback trên EDT thread
                SwingUtilities.invokeLater(() -> callback.onImageLoaded(icon));
            }
        });
        
        return null; // Trả về null, ảnh sẽ được load bất đồng bộ
    }
    
    /**
     * Load ảnh từ disk cache hoặc URL
     */
    private ImageIcon loadImageFromCacheOrUrl(String imageUrl, int width, int height, String cacheKey) throws Exception {
        // Tạo tên file cache từ URL hash
        String fileName = generateCacheFileName(imageUrl);
        File cacheFile = new File(CACHE_DIR, fileName);
        
        BufferedImage image = null;
        
        // 1. Thử load từ disk cache trước
        if (cacheFile.exists() && cacheFile.isFile()) {
            try {
                image = ImageIO.read(cacheFile);
                if (image != null) {
                    System.out.println("✓ Load ảnh từ disk cache: " + imageUrl);
                }
            } catch (IOException e) {
                System.err.println("Lỗi đọc cache file: " + e.getMessage());
                // Xóa file cache bị lỗi
                cacheFile.delete();
            }
        }
        
        // 2. Nếu không có trong disk cache, download từ internet
        if (image == null) {
            System.out.println("⬇ Downloading ảnh từ internet: " + imageUrl);
            try {
                java.net.URI uri = java.net.URI.create(imageUrl);
                URL url = uri.toURL();
                image = ImageIO.read(url);
                
                if (image != null) {
                    // Lưu vào disk cache cho lần sau
                    saveToDiskCache(image, cacheFile);
                    System.out.println("✓ Đã lưu ảnh vào disk cache: " + fileName);
                }
            } catch (Exception e) {
                System.err.println("Lỗi download ảnh từ " + imageUrl + ": " + e.getMessage());
                throw e;
            }
        }
        
        if (image == null) {
            return null;
        }
        
    // 3. Scale ảnh về kích thước mong muốn bằng phương pháp chất lượng cao
    BufferedImage scaledBuffered = getHighQualityScaledInstance(image, width, height, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
    ImageIcon icon = new ImageIcon(scaledBuffered);
        
        // 4. Lưu vào RAM cache
        memoryCache.put(cacheKey, icon);
        System.out.println("✓ Đã lưu ảnh vào RAM cache: " + cacheKey);
        
        return icon;
    }
    
    /**
     * Tạo tên file cache từ URL bằng MD5 hash
     */
    private String generateCacheFileName(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(url.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString() + ".png"; // Lưu dưới dạng PNG để bảo toàn chất lượng/alpha
        } catch (Exception e) {
            // Fallback: dùng hashCode nếu MD5 không khả dụng
            return String.valueOf(url.hashCode()) + ".jpg";
        }
    }
    
    /**
     * Lưu ảnh vào disk cache
     */
    private void saveToDiskCache(BufferedImage image, File cacheFile) {
        try {
            // Lưu dưới dạng PNG để giữ alpha và chất lượng ảnh
            // Nếu file extension không phải png, đổi thành png
            String fn = cacheFile.getName();
            if (!fn.toLowerCase().endsWith(".png")) {
                cacheFile = new File(cacheFile.getParentFile(), fn + ".png");
            }

            // Ensure parent exists
            File parent = cacheFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            ImageIO.write(image, "png", cacheFile);
        } catch (IOException e) {
            System.err.println("Lỗi lưu ảnh vào cache: " + e.getMessage());
        }
    }

    /**
     * High-quality image scaling using Graphics2D with rendering hints.
     * Uses incremental downscaling for better quality when reducing by large factors.
     */
    private BufferedImage getHighQualityScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object interpolationHint, boolean progressiveBilinear) {
        int type = img.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        int w = img.getWidth();
        int h = img.getHeight();

        if (w == targetWidth && h == targetHeight) {
            return img;
        }

        // If progressive bilinear, scale down in multiple steps for better quality
        if (progressiveBilinear) {
            int prevW = w;
            int prevH = h;
            BufferedImage intermediate = img;
            while (prevW > targetWidth || prevH > targetHeight) {
                prevW = Math.max(targetWidth, prevW / 2);
                prevH = Math.max(targetHeight, prevH / 2);
                BufferedImage tmp = new BufferedImage(prevW, prevH, type);
                Graphics2D g2 = tmp.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(intermediate, 0, 0, prevW, prevH, null);
                g2.dispose();
                intermediate = tmp;
                // break if we reached target to avoid infinite loop
                if (prevW == targetWidth && prevH == targetHeight) break;
            }
            // Final resize if needed
            if (intermediate.getWidth() != targetWidth || intermediate.getHeight() != targetHeight) {
                BufferedImage tmp = new BufferedImage(targetWidth, targetHeight, type);
                Graphics2D g2 = tmp.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(intermediate, 0, 0, targetWidth, targetHeight, null);
                g2.dispose();
                ret = tmp;
            } else {
                ret = intermediate;
            }
        } else {
            BufferedImage tmp = new BufferedImage(targetWidth, targetHeight, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(img, 0, 0, targetWidth, targetHeight, null);
            g2.dispose();
            ret = tmp;
        }

        return ret;
    }
    
    /**
     * Xóa cache (cả RAM và disk)
     */
    public void clearCache() {
        // Xóa RAM cache
        memoryCache.clear();
        System.out.println("✓ Đã xóa RAM cache");
        
        // Xóa disk cache
        try {
            File cacheDir = new File(CACHE_DIR);
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            file.delete();
                        }
                    }
                }
                System.out.println("✓ Đã xóa disk cache");
            }
        } catch (Exception e) {
            System.err.println("Lỗi xóa disk cache: " + e.getMessage());
        }
    }
    
    /**
     * Lấy thông tin cache hiện tại
     */
    public String getCacheInfo() {
        int ramCacheSize = memoryCache.size();
        int diskCacheSize = 0;
        long diskCacheSizeBytes = 0;
        
        try {
            File cacheDir = new File(CACHE_DIR);
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    diskCacheSize = files.length;
                    for (File file : files) {
                        if (file.isFile()) {
                            diskCacheSizeBytes += file.length();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi đọc thông tin cache: " + e.getMessage());
        }
        
        return String.format("RAM Cache: %d ảnh | Disk Cache: %d files (%.1f MB)", 
                ramCacheSize, diskCacheSize, diskCacheSizeBytes / (1024.0 * 1024.0));
    }

    /**
     * Remove cached entries (RAM + disk) for a specific image URL.
     * This helps when an avatar URL has changed on the server and we want
     * to force re-download the new image.
     */
    public void remove(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return;
        try {
            // remove RAM entries that start with imageUrl_
            String prefix = imageUrl + "_";
            memoryCache.keySet().removeIf(k -> k.startsWith(prefix));

            // remove disk file by hash name
            String fileName = generateCacheFileName(imageUrl);
            File f = new File(CACHE_DIR, fileName);
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa cache cho " + imageUrl + ": " + e.getMessage());
        }
    }
    
    /**
     * Interface callback cho việc load ảnh bất đồng bộ
     */
    public interface ImageLoadCallback {
        void onImageLoaded(ImageIcon icon);
    }
}