package com.simonscholz.services.monitoring;

import java.util.function.Consumer;

@FunctionalInterface
public interface UiFreezeConsumer extends Consumer<UiFreezeEvent> {
}
