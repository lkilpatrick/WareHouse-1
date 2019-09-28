/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.storage.mywarehouse;

import com.storage.mywarehouse.Dao.ProductDAO;
import com.storage.mywarehouse.Dao.QuantityHistoryViewDAO;
import com.storage.mywarehouse.Entity.Product;
import com.storage.mywarehouse.Entity.Warehouse;
import com.storage.mywarehouse.Hibernate.NewHibernateUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;

import static org.hibernate.criterion.Restrictions.and;
import static org.hibernate.criterion.Restrictions.eq;

/**
 * @author bojan, Patroklos
 */
public class Util {

    private enum ProductHeader {
        BRAND, TYPE, DESCRIPTION, PRICE
    }

    private enum WarehouseHeader {
        NAME
    }

    public static String parseProducts(File file) throws IOException {
        CSVParser csvParser = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.EXCEL.withHeader(ProductHeader.class));
        int numberOfSuccessfulRows = 0;
        int numberOfFailedRows = 0;
        for (CSVRecord record : csvParser.getRecords()) {
            Product product = parseProduct(record);
            if (product == null) {
                numberOfFailedRows++;
            } else {
                numberOfSuccessfulRows++;
            }
        }
        return "Number of successfully inserted rows: " + numberOfSuccessfulRows + "\n Number of erronous rows: " + numberOfFailedRows;
    }

    private static Product parseProduct(CSVRecord record) {
        String brand = record.get(ProductHeader.BRAND);
        String type = record.get(ProductHeader.TYPE);
        if ("".equals(brand) || "".equals(type)) {
            return null;
        }
        if (ProductDAO.findBy(brand, type) != null) {
            System.out.println("Product of brand " + brand + " and type " + type + " already exists!");
            return null;
        }

        String description = record.get(ProductHeader.DESCRIPTION);
        double price = Double.parseDouble(record.get(ProductHeader.PRICE));
        return ProductDAO.saveWith(brand, type, description, price);
    }

    public static String parseWarehouses(File file, mainframe frame) throws IOException {
        CSVParser csvParser = CSVParser.parse(file, StandardCharsets.UTF_8, CSVFormat.EXCEL.withHeader(WarehouseHeader.class));
        int numberOfSuccessfulRows = 0;
        int numberOfFailedRows = 0;
        for (CSVRecord record : csvParser.getRecords()) {
            String name = record.get(WarehouseHeader.NAME);
            if ("".equals(name)) {
                numberOfFailedRows++;
                continue;
            }

            if (addWarehouse(name, frame) < 0) {
                numberOfFailedRows++;
            } else {
                numberOfSuccessfulRows++;
            }

        }
        return "Number of successfully inserted rows: " + numberOfSuccessfulRows + "\n Number of erronous rows: " + numberOfFailedRows;
    }

    public static void exportQuantityHistory(File file) {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("Warehouse\tBrand\tType\tDate\tQuantity");
            for (Object qhv : QuantityHistoryViewDAO.findAll()) {
                out.println(qhv.toString());
            }
        } catch (IOException ex) {
            Logger.getLogger(mainframe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static int addWarehouse(String name, mainframe frame) {
        if (findWarehouseByName(name) != null) {
            System.out.println("Warehouse " + name + " already exists!");
            return -1;
        }

        Warehouse w = saveWarehouseWithName(name);
        List<storagepanel> panels = frame.getPanels();
        panels.add(new storagepanel(w, frame));
        frame.getWarehouses().add(w);
        frame.getTabs().add(name, panels.get(panels.size() - 1));
        return 0;
    }

    private static Warehouse saveWarehouseWithName(String name) {
        Session session = NewHibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        int warehouseId;
        Warehouse withHighestId = (Warehouse) session.createCriteria(Warehouse.class)
                .addOrder(Order.desc("warehouseId"))
                .setMaxResults(1)
                .uniqueResult();
        if (withHighestId == null) {
            warehouseId = 0;
        } else {
            warehouseId = withHighestId.getWarehouseId() + 1;
        }
        Warehouse w = new Warehouse(warehouseId, name);
        session.save(w);
        transaction.commit();
        session.close();
        return w;
    }

    private static Warehouse findWarehouseByName(String name) {
        Session session = NewHibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        Warehouse existingWarehouse = (Warehouse) session.createCriteria(Warehouse.class)
                .add(and(
                        eq("name", name)
                )).uniqueResult();
        transaction.commit();
        session.close();
        return existingWarehouse;
    }
}
