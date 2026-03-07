import "DPI-C" context function void ebreak(input int status);

module ebreak_dpi (
    input wire clk,
    input wire resetn,
    input wire valid,
    input wire [31:0] status
);
  always @(posedge clk) begin
    if (resetn & valid) begin
      ebreak(status);
    end
  end
endmodule

