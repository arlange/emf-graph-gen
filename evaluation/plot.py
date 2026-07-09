import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

# p, squares, load_ms, scan_ms, total_ms, is_median_of_3
data = [
    (1,     72,      17,   19,    31,  True),
    (5,     216,     21,   22,    45,  True),
    (10,    396,     32,   28,    52,  True),
    (50,    1836,    68,   60,    136, True),
    (100,   3636,    139,  79,    206, True),
    (500,   18036,   377,  289,   618, True),
    (1000,  36036,   549,  636,   1123, True),
    (5000,  180036,  1306, 9842,  10461, True),
    (10000, 360036,  2491, 43302, 43567, True),
    (50000, 1800036, None, None,  1872422, False),
]

squares = np.array([d[1] for d in data], dtype=float)
total_ms = np.array([d[4] for d in data], dtype=float)
total_s = total_ms / 1000.0
single_run = np.array([not d[5] for d in data])

# palette (dataviz skill reference palette, light mode)
BLUE = "#2a78d6"
INK = "#0b0b0b"
INK2 = "#52514e"
MUTED = "#898781"
GRID = "#e1e0d9"
SURFACE = "#fcfcfb"

fit_mask = squares >= 3636  # p >= 100, excludes JVM-startup-dominated tiny runs
k, logC = np.polyfit(np.log(squares[fit_mask]), np.log(total_s[fit_mask]), 1)

fig, ax = plt.subplots(figsize=(10.5, 4.8), facecolor=SURFACE)
ax.set_facecolor(SURFACE)
ax.grid(True, which="both", linewidth=0.6, color=GRID, zorder=0)
for spine in ax.spines.values():
    spine.set_visible(False)
ax.tick_params(colors=MUTED, labelsize=9)

ax.plot(squares, total_s, color=BLUE, linewidth=2, marker="o", markersize=5,
        markerfacecolor=SURFACE, markeredgecolor=BLUE, markeredgewidth=1.6, zorder=3)
ax.scatter(squares[single_run], total_s[single_run], s=46, facecolor=SURFACE,
           edgecolor=BLUE, linewidth=1.6, zorder=4)

# reference slope lines anchored at the p=100 point
x_ref = np.array([squares[fit_mask][0], squares[-1]])
y_lin = total_s[fit_mask][0] * (x_ref / x_ref[0]) ** 1.0
y_quad = total_s[fit_mask][0] * (x_ref / x_ref[0]) ** 2.0
ax.plot(x_ref, y_lin, color=MUTED, linewidth=1.2, linestyle=(0, (4, 3)), zorder=2)
ax.plot(x_ref, y_quad, color=MUTED, linewidth=1.2, linestyle=(0, (1, 2)), zorder=2)
ax.text(x_ref[-1] * 1.15, y_lin[-1], "O(n)", color=MUTED, fontsize=9, va="center")
ax.text(x_ref[-1] * 1.15, y_quad[-1] * 0.75, "O(n²)", color=MUTED, fontsize=9, va="center")

ax.text(squares[-1], total_s[-1] * 1.35,
        f"measured ≈ O(n$^{{{k:.1f}}}$)", color=BLUE, fontsize=9.5, ha="right", fontweight="medium")

ax.set_xscale("log")
ax.set_yscale("log")
ax.set_xlabel("Type-Squares checked (n)", color=INK2, fontsize=10)
ax.set_ylabel("wall-clock time (s)", color=INK2, fontsize=10)

fig.tight_layout(rect=[0, 0, 1, 0.76])

fig.suptitle("Checker runtime vs. model size", color=INK, fontsize=14.5, fontweight="bold", x=0.02, y=0.99, ha="left")
fig.text(0.02, 0.905,
          "log–log · dashed lines are O(n) / O(n²) references, not fits.\n"
          "Medians of 3 JVM runs (single run at n=1.8M); STO instances generated via StoConfiguration.\n"
          "Measured on AMD Ryzen 7 PRO 7840U (8C/16T), 30 GiB RAM, OpenJDK 21.0.8 (Temurin).",
          fontsize=8.6, color=MUTED, ha="left", va="top")

fig.savefig("/home/arne/git/emf-graph-gen/evaluation/sto_check_scaling.png", dpi=220, facecolor=SURFACE)
print("k (fit exponent, total time vs squares, p>=100) =", k)
print("saved plot")
