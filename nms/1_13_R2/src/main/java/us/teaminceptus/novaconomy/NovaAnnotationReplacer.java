package us.teaminceptus.novaconomy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.Range;
import revxrsal.commands.annotation.dynamic.AnnotationReplacer;
import revxrsal.commands.annotation.dynamic.Annotations;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collection;

class NovaAnnotationReplacer {

    static class BalanceToRange implements AnnotationReplacer<Balance> {

        @Override
        public @Nullable Collection<Annotation> replaceAnnotations(@NotNull AnnotatedElement annotatedElement, @NotNull Balance balance) {
            double min = 0.0;
            if (NovaConfig.getConfiguration().isNegativeBalancesEnabled()) {
                min = NovaConfig.getConfiguration().getMaxNegativeBalance();
            }

            Range range = Annotations.create(Range.class, "min", min);
            return Arrays.asList(range);
        }
    }

}
