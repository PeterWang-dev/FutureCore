import "DPI-C" function void ebreak(input int status);

module ebreak_dpi (
    input wire valid,
    input wire [31:0] status
);
  always_ff begin
    if (valid) begin
      ebreak(status);
    end
  end
endmodule
