package xyz.nucleoid.parties;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface PartyResult permits PartyResult.Success, PartyResult.Error {
    PartyResult map(Function<Party, PartyResult> mapper);

    void ifSuccessElse(Consumer<Party> onSuccess, Consumer<PartyError> onError);

    record Success(Party party) implements PartyResult {
        public Success {
            Objects.requireNonNull(party);
        }

        @Override
        public PartyResult map(Function<Party, PartyResult> mapper) {
            return mapper.apply(this.party);
        }

        @Override
        public void ifSuccessElse(Consumer<Party> onSuccess, Consumer<PartyError> onError) {
            onSuccess.accept(this.party);
        }
    }

    record Error(PartyError error) implements PartyResult {
        public Error {
            Objects.requireNonNull(error);
        }

        @Override
        public PartyResult map(Function<Party, PartyResult> mapper) {
            return this;
        }

        @Override
        public void ifSuccessElse(Consumer<Party> onSuccess, Consumer<PartyError> onError) {
            onError.accept(this.error);
        }
    }
}
