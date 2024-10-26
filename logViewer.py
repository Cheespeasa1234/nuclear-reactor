import pandas as pd
import matplotlib.pyplot as plt
import io

def plot_selected_columns(data, columns_to_plot=None):
    """
    Plot selected columns from the dataframe.
    
    Args:
        data: pandas DataFrame containing the log data
        columns_to_plot: list of column names to plot. If None, plots all columns
    """
    # Clean up column names by removing special characters
    data.columns = [col.replace('*', '').replace('**', '') for col in data.columns]
    
    # If no columns specified, plot all columns
    if columns_to_plot is None:
        columns_to_plot = data.columns
    
    # Verify that specified columns exist
    valid_columns = [col for col in columns_to_plot if col in data.columns]
    if len(valid_columns) == 0:
        print("No valid columns specified. Available columns:", list(data.columns))
        return
    
    # Create the plot
    plt.figure(figsize=(12, 6))
    
    # Plot each selected column
    for column in valid_columns:
        plt.plot(data.index, data[column], label=column, marker='o', markersize=4)
    
    # Customize the plot
    plt.title('Log Data Visualization', fontsize=14)
    plt.xlabel('Index', fontsize=12)
    plt.ylabel('Value', fontsize=12)
    plt.grid(True, linestyle='--', alpha=0.7)
    plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left')
    
    # Adjust layout to prevent legend from being cut off
    plt.tight_layout()
    
    # Show the plot
    plt.show()

# Read the data
df = pd.read_csv('log.csv')

# Plot all columns
plot_selected_columns(df, ["t", "n"])