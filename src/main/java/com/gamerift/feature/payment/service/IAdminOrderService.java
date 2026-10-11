package com.gamerift.feature.payment.service;

import java.util.List;

import com.gamerift.feature.payment.dto.AdminOrderResponse;

/** Admin service for the "Orders" screen of the admin panel. */
public interface IAdminOrderService {

    List<AdminOrderResponse> getAll();
}