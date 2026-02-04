package net.fosterlink.fosterlinkbackend.models.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

@GroupSequence({Order1.class, Default.class, Order2.class})
public interface OrderedChecks {
}
