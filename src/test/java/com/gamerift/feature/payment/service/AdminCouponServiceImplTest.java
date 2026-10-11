package com.gamerift.feature.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.gamerift.feature.payment.dto.CouponRequest;
import com.gamerift.feature.payment.dto.CouponResponse;
import com.gamerift.feature.payment.model.Coupon;
import com.gamerift.feature.payment.model.CouponType;
import com.gamerift.feature.payment.repository.CouponRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.ConflictException;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the admin coupon rules. Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class AdminCouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private AdminCouponServiceImpl service;

    private Coupon coupon(String id, String code) {
        Coupon c = new Coupon();
        c.setId(id);
        c.setCode(code);
        c.setType(CouponType.PERCENT);
        c.setValue(10);
        c.setMaxUses(10);
        return c;
    }

    private CouponRequest request(String code, CouponType type, int value) {
        CouponRequest r = new CouponRequest();
        r.setCode(code);
        r.setType(type);
        r.setValue(value);
        r.setMaxUses(10);
        return r;
    }

    @Test
    void getAll_sortedByCode() {
        when(couponRepository.findAll()).thenReturn(List.of(coupon("c2", "ZZZ99"), coupon("c1", "GG10"), coupon("c3", "MID10")));

        List<CouponResponse> result = service.getAll();

        assertEquals("GG10", result.get(0).getCode());
        assertEquals("MID10", result.get(1).getCode());
        assertEquals("ZZZ99", result.get(2).getCode());
    }

    @Test
    void create_upperCasesTheCodeAndStoresIt() {
        when(couponRepository.findByCode("GG10")).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        CouponResponse response = service.create(request(" gg10 ", CouponType.PERCENT, 10));

        assertEquals("GG10", response.getCode());
    }

    @Test
    void create_duplicateCode_throwsConflict() {
        when(couponRepository.findByCode("GG10")).thenReturn(Optional.of(coupon("c1", "GG10")));

        assertThrows(ConflictException.class, () -> service.create(request("gg10", CouponType.PERCENT, 10)));
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void update_toAnotherCouponsCode_throwsConflict() {
        when(couponRepository.findById("c1")).thenReturn(Optional.of(coupon("c1", "GG10")));
        when(couponRepository.findByCode("MID10")).thenReturn(Optional.of(coupon("c2", "MID10")));

        assertThrows(ConflictException.class, () -> service.update("c1", request("mid10", CouponType.PERCENT, 10)));
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void update_unknownCoupon_throwsNotFound() {
        when(couponRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update("zzz", request("GG10", CouponType.PERCENT, 10)));
    }

    @Test
    void delete_removesTheCoupon() {
        when(couponRepository.existsById("c1")).thenReturn(true);

        service.delete("c1");

        verify(couponRepository).deleteById("c1");
    }

    @Test
    void create_percentOverOneHundred_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.create(request("GG10", CouponType.PERCENT, 101)));
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void create_flatCouponOfZero_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.create(request("GG5", CouponType.FLAT, 0)));
    }

    @Test
    void expiresAt_isStored() {
        when(couponRepository.findByCode("GG10")).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));
        CouponRequest r = request("gg10", CouponType.PERCENT, 10);
        r.setExpiresAt(LocalDate.of(2030, 12, 31));

        CouponResponse response = service.create(r);

        assertEquals(LocalDate.of(2030, 12, 31), response.getExpiresAt());
    }
}