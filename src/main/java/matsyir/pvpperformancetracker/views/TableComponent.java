/*
 * Copyright (c) 2025, Matsyir <contact@matsyir.com>
 * Copyright (c) 2018, Jordan Atwood <jordan.atwood423@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package matsyir.pvpperformancetracker.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.util.Text;

// mostly yoinked from: https://github.com/runelite/runelite/pull/4375
// & https://gist.github.com/Nightfirecat/a1e2187ac0a1bd7b5ef40de76d96aad9
// thanks nightfirecat
//
// then adapted to our usage:
// patch custom 3-column behavior, to have a consistently centered middle column
//
// not sure if original behavior still works at this point, but I did my best to keep it intact &
// patch our behaviors on top of it in case we wanna use it alongside.
@Slf4j
public class TableComponent implements LayoutableRenderableEntity
{
    public enum TableAlignment
    {
        LEFT,
        CENTER,
        RIGHT
    }

    public enum TableRowStyle
    {
        DEFAULT, // default/original TableComponent behavior (maybe if it still works)
        PERCENTAGE_BASED, // 3 column hack fix: 0 is left column, 2 is right, 1 is middle/background. 0 & 2 each take up X% width
    }

    private class Cell
    {
        private String text;
        private TextComponentShadowless component;

        private Cell(String text, TextComponentShadowless component)
        {
            this.text = text;
            this.component = component;
        }
    }

    @Getter
    private final Rectangle bounds = new Rectangle();

    private ArrayList<Cell[]> cells = new ArrayList<>();
    private Cell[] line1;
    private TableAlignment[] columnAlignments;
    private Color[] columnColors;
    private int numCols;
    private int numRows;
    private TableRowStyle rowStyle;
    private final float centerWidthPercent = 1f;
    private final float leftWidthPercent;
    private final float rightWidthPercent;

    private TableAlignment defaultAlignment = TableAlignment.LEFT;
    private Color defaultColor = Color.WHITE;
    private Dimension gutter = new Dimension(3, 0);
    private Point preferredLocation = new Point();
    private Dimension preferredSize = new Dimension(ComponentConstants.STANDARD_WIDTH, 0);

    private boolean fontsInitialized = false;

    public TableComponent()
    {
        this(TableRowStyle.DEFAULT);
    }
    public TableComponent(TableRowStyle rowStyle)
    {
        this(rowStyle, 50, 50);
    }
    public TableComponent(TableRowStyle rowStyle, int leftWidthPercent, int rightWidthPercent)
    {
        super();

        if (leftWidthPercent + rightWidthPercent != 100)
        {
            throw new InvalidParameterException("TableComponent : leftWidthPercent & rightWidthPercent must add to 100");
        }

        this.rowStyle = rowStyle;
        this.leftWidthPercent = (float)leftWidthPercent / 100f;
        this.rightWidthPercent = (float)rightWidthPercent / 100f;
    }
    
    private boolean is3ColHackFix() { return numCols == 3 && rowStyle == TableRowStyle.PERCENTAGE_BASED; }

    public void updateLeftRightCells(String left, Color leftColor, String right, Color rightColor)
    {
        if (!is3ColHackFix()) { return; }

        line1[0].text = left;
        columnColors[0] = leftColor;
        line1[2].text = right;
        columnColors[2] = rightColor;
    }
    public void updateLeftCell(String left, Color leftColor)
    {
        if (!is3ColHackFix()) { return; }

        line1[0].text = left;
        columnColors[0] = leftColor;
    }
    public void updateLeftCellText(String left)
    {
        if (!is3ColHackFix()) { return; }

        line1[0].text = left;
    }
    public void updateRightCell(String right, Color rightColor)
    {
        if (!is3ColHackFix()) { return; }

        line1[2].text = right;
        columnColors[2] = rightColor;
    }
    public void updateRightCellText(String right)
    {
        if (!is3ColHackFix()) { return; }

        line1[2].text = right;
    }

    @Override
    public Dimension render(final Graphics2D graphics)
    {
        if (is3ColHackFix())
        {
            return renderFor3ColHackFix(graphics);
        }
        final FontMetrics metrics = graphics.getFontMetrics();
        final int[] columnWidths = getColumnWidths(metrics);
        int height = 0;
        TextComponentShadowless cellTextComponent;

        graphics.translate(preferredLocation.x, preferredLocation.y);

        for (int row = 0; row < numRows; row++)
        {
            int x = 0;
            int startingRowHeight = height;
            for (int col = 0; col < numCols; col++)
            {
                int y = startingRowHeight;
                final String[] lines = lineBreakText(getCellText(col, row), columnWidths[col], metrics);
                for (String line : lines)
                {
                    cellTextComponent = getCellComponent(col, row);
                    if (cellTextComponent == null) { continue; }

                    boolean middleColumnHackFix = is3ColHackFix() && col == 1;
                    final int alignmentOffset = getAlignedPosition(line, getColumnAlignment(col), columnWidths[col], metrics);

                    y += metrics.getHeight();

                    if (middleColumnHackFix && !fontsInitialized)
                    {
                        cellTextComponent.setFont(PanelFactory.getIndexFontFrom(metrics.getFont()));
                        fontsInitialized = true;
                    }

                    cellTextComponent.updatePosition(x + alignmentOffset,
                            y - (middleColumnHackFix ? Math.round((1 - PanelFactory.LINE_INDEX_LABEL_FONT_SCALE) * 9) : 0)
                    );
                    cellTextComponent.setText(line);
                    cellTextComponent.setColor(getColumnColor(col));
                    cellTextComponent.render(graphics);
                }
                height = Math.max(height, y);
                x += columnWidths[col] + gutter.width;
            }
            height += gutter.height;
        }

        graphics.translate(-preferredLocation.x, -preferredLocation.y);
        bounds.setLocation(preferredLocation);
        bounds.setSize(preferredSize.width, height);
        return bounds.getSize();
    }

    private Dimension renderFor3ColHackFix(final Graphics2D graphics)
    {
        final FontMetrics metrics = graphics.getFontMetrics();
        final int[] columnWidths = getColumnWidths(metrics);
        int height = 0;
        TextComponentShadowless cellTextComponent;

        graphics.translate(preferredLocation.x, preferredLocation.y);

        // first: draw the background layer/line, which is col=1
        for (int row = 0; row < numRows; row++)
        {
            int x = 0;
            int startingRowHeight = height;
            int col = 1;

            int y = startingRowHeight;
            final String[] lines = lineBreakText(getCellText(col, row), columnWidths[col], metrics);
            for (String line : lines)
            {
                cellTextComponent = getCellComponent(col, row);
                if (cellTextComponent == null) { continue; }

                final int alignmentOffset = getAlignedPosition(line, getColumnAlignment(col), columnWidths[col], metrics);

                y += metrics.getHeight();

                if (!fontsInitialized)
                {
                    cellTextComponent.setFont(PanelFactory.getIndexFontFrom(metrics.getFont()));
                    fontsInitialized = true;
                }

                cellTextComponent.updatePosition(x + alignmentOffset,
                        y - Math.round((1 - PanelFactory.LINE_INDEX_LABEL_FONT_SCALE) * 9)
                );
                cellTextComponent.setText(line);
                cellTextComponent.setColor(getColumnColor(col));
                cellTextComponent.render(graphics);
            }
            height = Math.max(height, y);
            x += columnWidths[col] + gutter.width;
            height += gutter.height;
        }

        height = 0; // reset height for next layer

        // now draw the 2 other columns as usual (skip middle column 1)
        for (int row = 0; row < numRows; row++)
        {
            int x = 0;
            int startingRowHeight = height;
            for (int col = 0; col < numCols; col++)
            {
                if (col == 1) { continue; }

                int y = startingRowHeight;
                final String[] lines = lineBreakText(getCellText(col, row), columnWidths[col], metrics);
                for (String line : lines)
                {
                    cellTextComponent = getCellComponent(col, row);
                    if (cellTextComponent == null) { continue; }

                    final int alignmentOffset = getAlignedPosition(line, getColumnAlignment(col), columnWidths[col], metrics);

                    y += metrics.getHeight();

                    cellTextComponent.updatePosition(x + alignmentOffset, y);
                    cellTextComponent.setText(line);
                    cellTextComponent.setColor(getColumnColor(col));
                    cellTextComponent.render(graphics);
                }
                height = Math.max(height, y);
                x += columnWidths[col] + gutter.width;
            }
            height += gutter.height;
        }

        graphics.translate(-preferredLocation.x, -preferredLocation.y);
        bounds.setLocation(preferredLocation);
        bounds.setSize(preferredSize.width, height);
        return bounds.getSize();
    }

    @Override
    public void setPreferredLocation(@Nonnull final Point location)
    {
        this.preferredLocation = location;
    }

    @Override
    public void setPreferredSize(@Nonnull final Dimension size)
    {
        this.preferredSize = size;
    }

    public void setDefaultColor(@Nonnull final Color color)
    {
        this.defaultColor = color;
    }

    public void setDefaultAlignment(@Nonnull final TableAlignment alignment)
    {
        this.defaultAlignment = alignment;
    }

    public void setGutter(@Nonnull final Dimension gutter)
    {
        this.gutter = gutter;
    }

    public void setColumnColors(@Nonnull Color... colors)
    {
        columnColors = colors;
    }

    public void setColumnAlignments(@Nonnull TableAlignment... alignments)
    {
        columnAlignments = alignments;
    }

    public void setColumnColor(final int col, final Color color)
    {
        assert columnColors.length > col;
        columnColors[col] = color;
    }

    public void setColumnAlignment(final int col, final TableAlignment alignment)
    {
        assert columnAlignments.length > col;
        columnAlignments[col] = alignment;
    }

    public void addRow(@Nonnull final String... cells)
    {
        numCols = Math.max(numCols, cells.length);
        numRows++;
        Cell[] newRow = new Cell[cells.length];
        for (int col = 0; col < cells.length; col++)
        {
            TextComponentShadowless cellComponent = new TextComponentShadowless(false, !is3ColHackFix() || col != 1);
            newRow[col] = new Cell(cells[col], cellComponent);
        }

        this.cells.add(newRow);
        line1 = this.cells.get(0);
    }

    public void addRows(@Nonnull final String[]... rows)
    {
        for (String[] row : rows)
        {
            addRow(row);
        }
        line1 = this.cells.get(0);
    }

    private Color getColumnColor(final int column)
    {
        if (columnColors == null
                || columnColors.length <= column
                || columnColors[column] == null)
        {
            return defaultColor;
        }
        return columnColors[column];
    }

    private TableAlignment getColumnAlignment(final int column)
    {
        if (columnAlignments == null
                || columnAlignments.length <= column
                || columnAlignments[column] == null)
        {
            return defaultAlignment;
        }
        return columnAlignments[column];
    }

    private Cell getCell(final int col, final int row)
    {
        assert col < numCols && row < numRows;

        if (cells.get(row).length < col)
        {
            return null;
        }
        return cells.get(row)[col];
    }

    private String getCellText(final int col, final int row)
    {
        assert col < numCols && row < numRows;

        Cell c = getCell(col, row);
        if (c == null)
        {
            return "";
        }
        return c.text;
    }
    private TextComponentShadowless getCellComponent(final int col, final int row)
    {
        assert col < numCols && row < numRows;

        Cell c = getCell(col, row);
        if (c == null)
        {
            return null;
        }
        return c.component;
    }

    private int[] getColumnWidths(final FontMetrics metrics)
    {
        if (numCols <= 0)
        {
            return new int[0];
        }

        int[] finalcolw = new int[numCols];     // final width of columns

        // 3colHackFix: simplified width calculation, dont need to do all that math for this basic percentage column thing
        if (is3ColHackFix() && preferredSize.width >= 4)
        {
            // middle column: to be drawn on its own layer, 100% width
            finalcolw[1] = (int)(preferredSize.width * centerWidthPercent);

            // left column: width * leftPercent
            finalcolw[0] = (int)(preferredSize.width * leftWidthPercent);

            // right column: remaining pixels, minus gutter*2 (idk why we need x2 but it works)
            finalcolw[2] = preferredSize.width - finalcolw[0] - (gutter.width * 2);

            return finalcolw;
        }

        // Based on https://stackoverflow.com/questions/22206825/algorithm-for-calculating-variable-column-widths-for-set-table-width
        int[] maxtextw = new int[numCols];      // max text width over all rows
        int[] maxwordw = new int[numCols];      // max width of longest word
        boolean[] flex = new boolean[numCols];  // is column flexible?
        boolean[] wrap = new boolean[numCols];  // can column be wrapped?

        for (int col = 0; col < numCols; col++)
        {
            for (int row = 0; row < numRows; row++)
            {
                final String cell = getCellText(col, row);
                final int cellWidth = getTextWidth(metrics, cell);

                maxtextw[col] = Math.max(maxtextw[col], cellWidth);
                for (String word : cell.split(" "))
                {
                    maxwordw[col] = Math.max(maxwordw[col], getTextWidth(metrics, word));
                }

                if (maxtextw[col] == cellWidth)
                {
                    wrap[col] = cell.contains(" ");
                }
            }
        }

        int left = preferredSize.width - (numCols - 1) * gutter.width;
        final double avg = (double) left / numCols;
        int nflex = 0;

        // Determine whether columns should be flexible and assign width of non-flexible cells
        for (int col = 0; col < numCols; col++)
        {
            // This limit can be adjusted as needed
            final double maxNonFlexLimit = 1.5 * avg;

            flex[col] = maxtextw[col] > maxNonFlexLimit;
            if (flex[col])
            {
                nflex++;
            }
            else
            {
                finalcolw[col] = maxtextw[col];
                left -= finalcolw[col];
            }
        }

        // If there is not enough space, make columns that could be word-wrapped flexible too
        if (left < nflex * avg)
        {
            for (int col = 0; col < numCols; col++)
            {
                if (!flex[col] && wrap[col])
                {
                    left += finalcolw[col];
                    finalcolw[col] = 0;
                    flex[col] = true;
                    nflex++;
                }
            }
        }

        // Calculate weights for flexible columns. The max width is capped at the table width to
        // treat columns that have to be wrapped more or less equal
        int tot = 0;
        for (int col = 0; col < numCols; col++)
        {
            if (flex[col])
            {
                maxtextw[col] = Math.min(maxtextw[col], preferredSize.width);
                tot += maxtextw[col];
            }
        }

        // Now assign the actual width for flexible columns. Make sure that it is at least as long
        // as the longest word length
        for (int col = 0; col < numCols; col++)
        {
            if (flex[col])
            {
                finalcolw[col] = left * maxtextw[col] / tot;
                finalcolw[col] = Math.max(finalcolw[col], maxwordw[col]);
                left -= finalcolw[col];
            }
        }

        // When the sum of column widths is less than the total space available, distribute the
        // extra space equally across all columns
        final int extraPerCol = left / numCols;
        for (int col = 0; col < numCols; col++)
        {
            finalcolw[col] += extraPerCol;
            left -= extraPerCol;
        }
        // Add any remainder to the right-most column
        finalcolw[finalcolw.length - 1] += left;

        return finalcolw;
    }

    private static int getTextWidth(final FontMetrics metrics, final String cell)
    {
        return metrics.stringWidth(Text.removeTags(cell));
    }

    private static String[] lineBreakText(final String text, final int maxWidth, final FontMetrics metrics)
    {
        final String[] words = text.split(" ");

        if (words.length == 0)
        {
            return new String[0];
        }

        final StringBuilder wrapped = new StringBuilder(words[0]);
        int spaceLeft = maxWidth - getTextWidth(metrics, wrapped.toString());

        for (int i = 1; i < words.length; i++)
        {
            final String word = words[i];
            final int wordLen = getTextWidth(metrics, word);
            final int spaceWidth = metrics.stringWidth(" ");

            if (wordLen + spaceWidth > spaceLeft)
            {
                wrapped.append('\n').append(word);
                spaceLeft = maxWidth - wordLen;
            }
            else
            {
                wrapped.append(' ').append(word);
                spaceLeft -= spaceWidth + wordLen;
            }
        }

        return wrapped.toString().split("\n");
    }

    private static int getAlignedPosition(final String str, final TableAlignment alignment, final int columnWidth, final FontMetrics metrics)
    {
        final int stringWidth = getTextWidth(metrics, str);
        int offset = 0;

        switch (alignment)
        {
            case LEFT:
                break;
            case CENTER:
                offset = (columnWidth / 2) - (stringWidth / 2);
                break;
            case RIGHT:
                offset = columnWidth - stringWidth;
                break;
        }
        return offset;
    }
}