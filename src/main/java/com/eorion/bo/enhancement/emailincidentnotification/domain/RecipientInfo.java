package com.eorion.bo.enhancement.emailincidentnotification.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public record RecipientInfo(String receiver, String cc) {

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        RecipientInfo that = (RecipientInfo) o;

        return new EqualsBuilder().append(receiver, that.receiver).append(cc, that.cc).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(receiver).append(cc).toHashCode();
    }
}
