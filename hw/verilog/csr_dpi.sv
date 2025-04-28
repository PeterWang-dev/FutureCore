import "DPI-C" function void ebreak();

module csr_dpi (
  input wire valid
);
  always_ff begin
    if (valid) begin
      ebreak();
    end
  end
endmodule
