package net.blockomorph.utils.accessors;

import net.blockomorph.utils.use.UseController;

public interface BlockEntityAccessor {
    UseController getController();
    void setUseController(UseController ctr);
}
