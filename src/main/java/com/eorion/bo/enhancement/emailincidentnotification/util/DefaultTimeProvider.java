package com.eorion.bo.enhancement.emailincidentnotification.util;

import java.util.Date;

public class DefaultTimeProvider implements ITimeProvider{
    @Override
    public Date now() {
        return new Date();
    }
}
