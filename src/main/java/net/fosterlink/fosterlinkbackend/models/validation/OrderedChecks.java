package net.fosterlink.fosterlinkbackend.models.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/** Defines the order of validation groups: Order1, then Default, then Order2. */
@GroupSequence({Order1.class, Default.class, Order2.class})
public interface OrderedChecks {
}
