

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;

import java.awt.Color;
import java.io.FileReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@SuppressWarnings("serial")
public class TrendLineAnalysis extends JFrame{

    public TrendLineAnalysis(String title) {
        super(title);

        //Load Dataset
        List<CSVRecord> records = loadDataset("C:\\Users\\ompat\\IdeaProjects\\TrendLines Proj\\src\\StockDataHDFCBANK.csv");

        //Load XY dataset
        XYSeries closingPriceSeries = createDataset(records);

        //Calculate Trendline slope and Intercept
        double[] trendlineParams = calculateTrendline(closingPriceSeries);

        //Calculate support and resistance levels
        double[] supportResistance = calculateSupportResistance(closingPriceSeries);

        //Create chart
        JFreeChart chart = createChart(closingPriceSeries, trendlineParams, supportResistance);

        //Display Chart
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        setContentPane(chartPanel);
    }

    private List<CSVRecord> loadDataset(String filePath) {
        try (Reader reader = new FileReader(filePath);
             @SuppressWarnings("deprecation")
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            return parser.getRecords();
        }   catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private XYSeries createDataset(List<CSVRecord> records){
        XYSeries series = new XYSeries("Closing Price");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (CSVRecord record: records) {
            Date date;
            try {
                date = dateFormat.parse(record.get("Date"));
                double closingPrice = Double.parseDouble(record.get("Close"));
                series.add(date.getTime(), closingPrice);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return series;
    }

    private double[] calculateTrendline(XYSeries series) {
        //Simple linear regression to calculate trend line
        int n = series.getItemCount();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = series.getX(i).doubleValue();
            double y = series.getY(i).doubleValue();

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n ;

        return new double[]{slope, intercept};
    }

    private double[] calculateSupportResistance(XYSeries series) {
        //Calculate support and resistance
        double maxClosingPrice = series.getMaxY();
        double minClosingPrice = series.getMinY();

        return new double[]{minClosingPrice, maxClosingPrice};
    }

    private JFreeChart createChart(XYSeries series, double[] trendlineParams, double[] supportResistance) {
        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart("HDFC Stock Price Trendline Analysis", "Date", "Closing Price", dataset, true, true, false);

        XYPlot plot = chart.getXYPlot();

        //Add Trendline
        double slope = trendlineParams[0];
        double intercept = trendlineParams[1];
        addTrendLine(plot, series, slope, intercept);

        // Set base color for the entire plot (including legend)
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setBasePaint(Color.BLACK);

        //Add support and resistance lines
        double support = supportResistance[0];
        double resistance = supportResistance[1];

        //Add UP Trendline
        addUpTrendLine(plot, series);

        //Add Down Trendline
        addDownTrendLine(plot, series);

        //Add Support and Resistance lines to the legend
        addSupportResistanceToLegend(plot, support, resistance, series);

        //Customize Legend
        renderer.setBaseShapesVisible(true);
        renderer.setBaseShapesFilled(true);

        return chart;
    }

    private void addSupportResistanceToLegend(XYPlot plot, double support, double resistance, XYSeries series) {
        //Add support line to legend
        XYSeries supportSeries = new XYSeries("Support Line");
        supportSeries.add(series.getMinX(), support);
        supportSeries.add(series.getMaxX(), support);
        //plot.setDataset(plot.getDatasetCount(), new  XYSeriesCollection(supportSeries));
        //plot.setRenderer(plot.getDatasetCount() - 1, new XYLineAndShapeRenderer(true, false));

        //Add support line to the plot
        XYSeriesCollection dataset = new XYSeriesCollection(supportSeries);
        plot.setDataset(plot.getDatasetCount(), dataset);

        //Set color for support line
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, Color.MAGENTA);
        plot.setRenderer(plot.getDatasetCount() - 1, renderer);


        //Add resistance line to the legend
        XYSeries resistanceSeries =  new XYSeries("Resistance Line");
        resistanceSeries.add(series.getMinX(), resistance);
        resistanceSeries.add(series.getMaxX(), resistance);
        //plot.setDataset(plot.getDatasetCount(), new XYSeriesCollection(resistanceSeries));
        //plot.setRenderer(plot.getDatasetCount() - 1, new XYLineAndShapeRenderer(true, false));
        XYSeriesCollection dataset1 = new XYSeriesCollection(resistanceSeries);
        plot.setDataset(plot.getDatasetCount(), dataset1);

        //Set color for resistance line
        XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer(true, false);
        renderer1.setSeriesPaint(0, Color.GREEN);
        plot.setRenderer(plot.getDatasetCount() - 1, renderer1);
    }

    private void addUpTrendLine(XYPlot plot, XYSeries series) {
        double x1 = series.getX(0).doubleValue();
        double y1 = series.getY(0).doubleValue();

        double x2 = series.getX(series.getItemCount() - 1).doubleValue();
        double y2 = series.getY(series.getItemCount() - 1).doubleValue();

        XYSeries upTrendLineSeries = new XYSeries("Up Trend Line");
        upTrendLineSeries.add(x1, y1);
        upTrendLineSeries.add(x2, y2);

        //Add Up Trend Line to the plot
        XYSeriesCollection dataset = new XYSeriesCollection(upTrendLineSeries);
        plot.setDataset(plot.getDatasetCount(), dataset);

        //Set Color for Up Trend to Green
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, Color.BLACK);
        plot.setRenderer(plot.getDatasetCount() - 1, renderer);
    }

    private void addDownTrendLine(XYPlot plot, XYSeries series) {
        double x1 = series.getMinX();
        double y1 = series.getMaxY();
        double x2 = series.getMaxX();
        double y2 = series.getMinY();

        XYSeries downTrendLineSeries = new XYSeries("Down Trend Line");
        downTrendLineSeries.add(x1, y1);
        downTrendLineSeries.add(x2, y2);

        //Add Down Trend Line to the plot
        XYSeriesCollection dataset = new XYSeriesCollection(downTrendLineSeries);
        plot.setDataset(plot.getDatasetCount(), dataset);

        //Set color for Down Trend to pink
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, Color.YELLOW);
        plot.setRenderer(plot.getDatasetCount() - 1, renderer);
    }

    private void addTrendLine(XYPlot plot, XYSeries series, double slope, double intercept) {
        //Create Trendline Series
        XYSeries trendlineSeries = new XYSeries("Trendline");

        LocalDateTime firstDate = LocalDateTime.ofInstant(new Date((long) series.getMinX()).toInstant(), ZoneId.systemDefault());
        LocalDateTime lastDate = LocalDateTime.ofInstant(new Date((long) series.getMinX()).toInstant(), ZoneId.systemDefault());

        for (LocalDateTime date = firstDate; date.isBefore(lastDate.plusDays(1)); date = date.plusDays(1)) {
            double x = Date.from(date.atZone(ZoneId.systemDefault()).toInstant()).getTime();
            double y = slope * x + intercept;
            trendlineSeries.add(x, y);
        }

        // Add trendline to the plot
        plot.setDataset(plot.getDatasetCount(), new XYSeriesCollection(trendlineSeries));
        plot.setRenderer(plot.getDatasetCount()- 1, new XYLineAndShapeRenderer(true, false));
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(()-> {
            TrendLineAnalysis example = new TrendLineAnalysis("HDFC Bank Stock Price Analysis");
            example.setSize(800,600);
            example.setLocationRelativeTo(null);
            example.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            example.setVisible(true);
        });
    }
}
