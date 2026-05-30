package com.ecoamazonas.eco_agua.marketing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {

    List<Testimonial> findTop5ByActiveTrueOrderByDisplayOrderAscIdAsc();

    List<Testimonial> findAllByOrderByDisplayOrderAscIdAsc();
}
