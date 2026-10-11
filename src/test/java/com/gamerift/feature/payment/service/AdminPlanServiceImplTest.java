package com.gamerift.feature.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.payment.dto.PlanRequest;
import com.gamerift.feature.payment.dto.PlanResponse;
import com.gamerift.feature.payment.model.Plan;
import com.gamerift.feature.payment.repository.PlanRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the admin plan rules. Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class AdminPlanServiceImplTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private AdminPlanServiceImpl service;

    private Plan plan(String id, String name, int price) {
        Plan p = new Plan();
        p.setId(id);
        p.setName(name);
        p.setPriceBdt(price);
        p.setActive(true);
        return p;
    }

    private PlanRequest request(String name) {
        PlanRequest r = new PlanRequest();
        r.setName(name);
        r.setDays(30);
        r.setPriceBdt(499);
        r.setTagline("Pro");
        return r;
    }

    @Test
    void getAll_sortsByPriceAscending() {
        when(planRepository.findAll()).thenReturn(List.of(plan("p3", "Yearly", 1999), plan("p1", "Weekly", 149), plan("p2", "Monthly", 299)));

        List<PlanResponse> result = service.getAll();

        assertEquals("p1", result.get(0).getId());
        assertEquals("p2", result.get(1).getId());
        assertEquals("p3", result.get(2).getId());
    }

    @Test
    void create_copiesTheRequest() {
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanResponse response = service.create(request("Monthly"));

        assertEquals("Monthly", response.getName());
        assertEquals(30, response.getDays());
        assertEquals("Pro", response.getTagline());
    }

    @Test
    void update_changesTheExistingPlan() {
        when(planRepository.findById("p1")).thenReturn(Optional.of(plan("p1", "Monthly", 299)));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanResponse response = service.update("p1", request("Yearly"));

        assertEquals("Yearly", response.getName());
    }

    @Test
    void update_unknownPlan_throwsNotFound() {
        when(planRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update("zzz", request("Yearly")));
    }

    @Test
    void delete_softDeletesSoTheHistoryKeepsReading() {
        Plan p = plan("p1", "Monthly", 299);
        when(planRepository.findById("p1")).thenReturn(Optional.of(p));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete("p1");

        assertFalse(p.isActive());
        verify(planRepository).save(p);
    }

    @Test
    void delete_unknownPlan_throwsNotFound() {
        when(planRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete("zzz"));
    }
}