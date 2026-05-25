package com.habeshabank.ui;

/**
 * Implemented by content panels that display live account or transaction data.
 * {@link com.habeshabank.ui.dashboard.MainFrame#refreshAllUI()} invokes
 * {@link #refreshData()} on every registered panel after a financial operation.
 */
public interface Refreshable {

    /** Reload displayed values from the current session / transaction log. */
    void refreshData();
}
