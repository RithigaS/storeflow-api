package com.grootan.storeflow.service;

import com.grootan.storeflow.entity.Order;

public interface OrderReportPdfService {

    byte[] generateOrderReport(Order order);
}