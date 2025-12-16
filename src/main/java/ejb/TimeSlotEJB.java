package ejb;

import entities.ArtistSchedule;
import entities.TimeSlot;
import entities.AppUser;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class TimeSlotEJB {

    @PersistenceContext
    private EntityManager em;

    // Generate slots for a given artist & date
    public void generateSlotsForDate(AppUser artist, LocalDate date, int slotDurationMinutes) {
        // Fetch schedule for this day
        ArtistSchedule schedule = em.createQuery(
                "SELECT s FROM ArtistSchedule s WHERE s.artist = :artist AND s.dayOfWeek = :dow AND s.isWorking = true",
                ArtistSchedule.class)
                .setParameter("artist", artist)
                .setParameter("dow", date.getDayOfWeek())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (schedule == null) return;

        List<TimeSlot> slots = new ArrayList<>();
        LocalTime start = schedule.getStartTime();
        LocalTime end = schedule.getEndTime();

        while (!start.plusMinutes(slotDurationMinutes).isAfter(end)) {
            TimeSlot slot = new TimeSlot();
            slot.setArtist(artist);
            slot.setStartTime(LocalDateTime.of(date, start));
            slot.setEndTime(LocalDateTime.of(date, start.plusMinutes(slotDurationMinutes)));
            slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);

            slots.add(slot);
            start = start.plusMinutes(slotDurationMinutes);
        }

        slots.forEach(em::persist);
    }
}
