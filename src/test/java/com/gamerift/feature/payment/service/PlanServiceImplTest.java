package com.gamerift.feature.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import com.gamerift.feature.payment.dto.PlanResponse;
import com.gamerift.feature.payment.model.Plan;
import com.gamerift.feature.payment.repository.PlanRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the public plan list. Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class PlanServiceImplTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanServiceImpl service;

    private Plan plan(String id, String name, int price, boolean active) {
        Plan p = new Plan();
        p.setId(id);
        p.setName(name);
        p.setPriceBdt(price);
        p.setActive(active);
        return p;
    }

    @Test
    void getActivePlans_filtersInactiveAndSortsCheapestFirst() {
        when(planRepository.findAll()).thenReturn(List.of(
                plan("p3", "Yearly", 1999, true),
                plan("p1", "Weekly", 149, true),
                plan("p2", "Monthly", 499, false)));

        List<PlanResponse> result = service.getActivePlans();

        assertEquals(2, result.size());
        assertEquals("p1", result.get(0).getId());
        assertEquals("p3", result.get(1).getId());
    }

    @Test
    void getActivePlans_noPlans_returnsEmptyList() {
        when(planRepository.findAll()).thenReturn(List.of());

        List<PlanResponse> result = service.getActivePlans();

        assertEquals(0, result.size());
    }
}