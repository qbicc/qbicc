package org.qbicc.type.definition;

import java.util.List;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Slot;

/**
 *
 */
public interface MethodBody {

    /**
     * Get the number of positional parameters.
     *
     * @return the number of positional parameters
     */
    int getParameterCount();

    /**
     * Get the slot for a positional parameter.
     *
     * @param index the parameter position
     * @return the slot (not {@code null})
     * @throws IndexOutOfBoundsException if {@code index} is less than zero or is greater than or equal to the number of positional parameters
     */
    Slot getParameterSlot(int index) throws IndexOutOfBoundsException;

    /**
     * Get the list of positional parameter slots in order.
     * The slot determines how positional parameters are mapped to block arguments.
     *
     * @return the slots list
     */
    List<Slot> getParameterSlots();

    BasicBlock getEntryBlock();

    static MethodBody of(BasicBlock entryBlock, List<Slot> paramSlots) {
        return new MethodBody() {
            public int getParameterCount() {
                return paramSlots.size();
            }

            @Override
            public Slot getParameterSlot(int index) throws IndexOutOfBoundsException {
                return paramSlots.get(index);
            }

            @Override
            public List<Slot> getParameterSlots() {
                return paramSlots;
            }

            public BasicBlock getEntryBlock() {
                return entryBlock;
            }

        };
    }
}
