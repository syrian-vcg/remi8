package com.remi8.engine.scene;

import android.graphics.*;
import com.remi8.engine.renderer.Renderer2D;
import java.util.HashMap;
import java.util.Map;

/**
 * خريطة البلاطات (TileMap) - REMI8
 * تُستخدم لبناء مستويات اللعبة من بلاطات صغيرة
 */
public class TileMap {

    private int[][] mapData;           // بيانات الخريطة [صف][عمود]
    private int tileWidth, tileHeight; // حجم كل بلاطة بالبكسل
    private int mapCols, mapRows;      // أبعاد الخريطة بالبلاطات

    // صور البلاطات
    private Bitmap tileSheet;
    private int sheetCols;
    private final Map<Integer, Rect> tileRects = new HashMap<>();

    // الرسم
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint debugPaint = new Paint();

    // إزاحة الخريطة
    private float offsetX = 0, offsetY = 0;

    public TileMap(int cols, int rows, int tileW, int tileH) {
        this.mapCols = cols;
        this.mapRows = rows;
        this.tileWidth = tileW;
        this.tileHeight = tileH;
        this.mapData = new int[rows][cols];

        debugPaint.setColor(Color.argb(80, 100, 100, 200));
        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setStrokeWidth(1f);
    }

    /**
     * تعيين صورة البلاطات
     */
    public void setTileSheet(Bitmap sheet, int sheetCols) {
        this.tileSheet = sheet;
        this.sheetCols = sheetCols;
        buildTileRects();
    }

    private void buildTileRects() {
        if (tileSheet == null) return;
        int sheetRows = tileSheet.getHeight() / tileHeight;
        for (int r = 0; r < sheetRows; r++) {
            for (int c = 0; c < sheetCols; c++) {
                int id = r * sheetCols + c + 1;
                tileRects.put(id, new Rect(
                    c * tileWidth, r * tileHeight,
                    (c + 1) * tileWidth, (r + 1) * tileHeight
                ));
            }
        }
    }

    /**
     * تعيين بلاطة في موضع محدد
     */
    public void setTile(int col, int row, int tileId) {
        if (row >= 0 && row < mapRows && col >= 0 && col < mapCols) {
            mapData[row][col] = tileId;
        }
    }

    public int getTile(int col, int row) {
        if (row < 0 || row >= mapRows || col < 0 || col >= mapCols) return 0;
        return mapData[row][col];
    }

    /**
     * ملء صف كامل
     */
    public void fillRow(int row, int tileId) {
        for (int c = 0; c < mapCols; c++) setTile(c, row, tileId);
    }

    /**
     * ملء عمود كامل
     */
    public void fillCol(int col, int tileId) {
        for (int r = 0; r < mapRows; r++) setTile(col, r, tileId);
    }

    /**
     * ملء مستطيل من البلاطات
     */
    public void fillRect(int startCol, int startRow, int endCol, int endRow, int tileId) {
        for (int r = startRow; r <= endRow; r++)
            for (int c = startCol; c <= endCol; c++)
                setTile(c, r, tileId);
    }

    /**
     * تحميل خريطة من مصفوفة
     */
    public void loadFromArray(int[][] data) {
        for (int r = 0; r < Math.min(data.length, mapRows); r++)
            for (int c = 0; c < Math.min(data[r].length, mapCols); c++)
                mapData[r][c] = data[r][c];
    }

    /**
     * رندرة الخريطة
     */
    public void render(Canvas canvas, float camX, float camY, float zoom, boolean debug) {
        if (canvas == null) return;

        int startCol = Math.max(0, (int) ((camX - offsetX) / (tileWidth * zoom)));
        int startRow = Math.max(0, (int) ((camY - offsetY) / (tileHeight * zoom)));
        int endCol = Math.min(mapCols, startCol + (int) (canvas.getWidth() / (tileWidth * zoom)) + 2);
        int endRow = Math.min(mapRows, startRow + (int) (canvas.getHeight() / (tileHeight * zoom)) + 2);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                int tileId = mapData[r][c];
                if (tileId == 0) continue;

                float drawX = (offsetX + c * tileWidth - camX) * zoom;
                float drawY = (offsetY + r * tileHeight - camY) * zoom;
                float drawW = tileWidth * zoom;
                float drawH = tileHeight * zoom;

                RectF dest = new RectF(drawX, drawY, drawX + drawW, drawY + drawH);

                if (tileSheet != null && tileRects.containsKey(tileId)) {
                    canvas.drawBitmap(tileSheet, tileRects.get(tileId), dest, tilePaint);
                } else {
                    // لون افتراضي حسب ID
                    tilePaint.setColor(getTileColor(tileId));
                    tilePaint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(dest, tilePaint);
                }

                if (debug) canvas.drawRect(dest, debugPaint);
            }
        }
    }

    private int getTileColor(int id) {
        int[] colors = {
            Color.parseColor("#4a4a6a"), // 1 - أرضية
            Color.parseColor("#2a6a2a"), // 2 - عشب
            Color.parseColor("#8B4513"), // 3 - تربة
            Color.parseColor("#6a6a8a"), // 4 - صخر
            Color.parseColor("#4a8a4a"), // 5 - نبات
            Color.parseColor("#2a2a8a"), // 6 - ماء
            Color.parseColor("#ffd700"), // 7 - عملة
        };
        int idx = (id - 1) % colors.length;
        return colors[idx];
    }

    /**
     * التحقق من التصادم مع الخريطة
     */
    public boolean isSolid(float worldX, float worldY) {
        int col = (int) ((worldX - offsetX) / tileWidth);
        int row = (int) ((worldY - offsetY) / tileHeight);
        int tileId = getTile(col, row);
        return tileId > 0 && tileId != 6 && tileId != 7; // ماء وعملة ليست صلبة
    }

    // الأبعاد
    public float getPixelWidth()  { return mapCols * tileWidth; }
    public float getPixelHeight() { return mapRows * tileHeight; }
    public int getMapCols() { return mapCols; }
    public int getMapRows() { return mapRows; }
    public int getTileWidth() { return tileWidth; }
    public int getTileHeight() { return tileHeight; }
    public void setOffset(float x, float y) { offsetX = x; offsetY = y; }
    public int[][] getMapData() { return mapData; }
}
