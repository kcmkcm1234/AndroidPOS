package com.project.sean.androidpos;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.project.sean.androidpos.Database.AndroidPOSDBHelper;
import com.project.sean.androidpos.Database.SaleInfo;
import com.project.sean.androidpos.Database.StockSale;
import com.project.sean.androidpos.cart.CartItem;
import com.project.sean.androidpos.cart.ShoppingCart;
import com.project.sean.androidpos.cart.ShoppingCartItem;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This activity deals with the buying of goods from the shop.
 * Created by Sean on 29/04/2016.
 */
public class CheckoutActivity extends AppCompatActivity implements View.OnClickListener {
    //Instance of the database
    private AndroidPOSDBHelper dbHelper;

    // User Session Manager Class
    UserSessionManager session;

    private CheckoutAdapter mAdapter;

    private ShoppingCart shoppingCart;

    //private ArrayList<ShoppingCartItem> cartItemList;

    private ListView lvCheckoutItems;

    //EditText to enter stockID ID
    private EditText editCheckoutStockId;

    //Button to scan in an item of stock
    private Button btCheckoutScan;
    //Button for cash payment
    private Button bCash;
    //Button for card payment
    private Button bCard;

    //TextView of total
    private TextView tvTotalPrice;

    private int currentEmpId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        session = new UserSessionManager(getApplicationContext());

        // get user data from session
        HashMap<String, String> user = session.getUserDetails();

        currentEmpId = Integer.parseInt(user.get(UserSessionManager.KEY_EMPID));

        //Get instance of the DB
        dbHelper = AndroidPOSDBHelper.getInstance(this);

        shoppingCart = new ShoppingCart();

        lvCheckoutItems = (ListView) findViewById(R.id.lvCheckoutItems);
        LayoutInflater layoutInflater = getLayoutInflater();

        lvCheckoutItems.addHeaderView(layoutInflater.inflate(R.layout.cart_header, lvCheckoutItems, false));

        //Create the array adapter for the shopping cart
        mAdapter = new CheckoutAdapter(this, R.layout.adapter_cart_item, shoppingCart.getCartItems());

        //Add the adapter to the ListView
        lvCheckoutItems.setAdapter(mAdapter);

        //Add a context menu to the list view
        registerForContextMenu(lvCheckoutItems);

        //Scan a product into the cart
        btCheckoutScan = (Button) findViewById(R.id.btCheckoutScan);
        bCash = (Button) findViewById(R.id.bCash);
        bCard = (Button) findViewById(R.id.bCard);

        //EditText for product input
        editCheckoutStockId = (EditText) findViewById(R.id.editCheckoutStockId);

        //Displays the initial total
        tvTotalPrice = (TextView) findViewById(R.id.tvTotalPrice);
        tvTotalPrice.setText(currencyOut(shoppingCart.getSubtotal()).toString());

        //OnClickListener for buttons
        btCheckoutScan.setOnClickListener(this);
        bCash.setOnClickListener(this);
        bCard.setOnClickListener(this);

        editCheckoutStockId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEND) {
                    if(dbHelper.exsists(v.getText().toString())){
                        //TO-DO add a connection to the database
                        addItemToCart(v.getText().toString());
                        editCheckoutStockId.getText().clear();
                    } else {
                        Toast.makeText(CheckoutActivity.this, "No stock item found for ID: " + v.getText(),
                                Toast.LENGTH_SHORT).show();
                    }

                    return true;
                }
                return false;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_checkout, menu);
        //Set the title
        setTitle(getString(R.string.checkout_activity_title));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_return_item:
                Intent intent = new Intent(CheckoutActivity.this, ReturnItemActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_empty_cart:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                confirmEmptyCart();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btCheckoutScan: {
                IntentIntegrator scanIntegrator = new IntentIntegrator(this);
                scanIntegrator.initiateScan();
                break;
            }
            case R.id.bCash: {
                //TO-DO add a way to pay by cash
                confirmCashPayment();
                break;
            }
            case R.id.bCard: {
                //TO-DO add a way to pay by card
                confirmCardPayment();
                break;
            }
        }
    }

    /**
     * Handles all Scan requests and results.
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {
            if(scanningResult.getContents() == null) {
                Toast.makeText(this, "Cancelled from fragment!", Toast.LENGTH_SHORT);
            } else {
                String scanContent = scanningResult.getContents();
                if (dbHelper.exsists(scanContent)) {
                    //TO-DO get the stock information from the database
                    //Then add the item to the cart
                    addItemToCart(scanContent);
                } else {
                    Toast.makeText(this, "No stock item found for ID: " + scanContent,
                            Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this,"No scan data received!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Adds an item from the database to the shopping cart, then refreshes the list view.
     * @param stockId
     */
    public void addItemToCart(String stockId) {
        Cursor result = dbHelper.getStockDetails(stockId);

        CartItem newItem = new CartItem();
        newItem.setStockId(result.getString(0));
        newItem.setStockName(result.getString(1));
        newItem.setSalePrice(result.getInt(2));

        shoppingCart.addItem(newItem);
        ShoppingCartItem tempItem = new ShoppingCartItem(newItem);
        //mAdapter.add(tempItem);
        //working solution
        mAdapter.stockList.add(tempItem);
        mAdapter.notifyDataSetChanged();

        tvTotalPrice.setText(currencyOut(shoppingCart.getSubtotal()).toString());
        //result.close();
    }

    /**
     * Confirm if you want to empty the cart.
     */
    private void confirmEmptyCart() {
        if(!shoppingCart.getCartItems().isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder
                    .setMessage("Empty the shopping cart?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            shoppingCart.clearCart();
                            mAdapter.notifyDataSetChanged();
                            tvTotalPrice.setText(currencyOut(shoppingCart.getSubtotal()).toString());
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .show();
        } else {
            Toast.makeText(this, "Checkout is empty.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Confirm if you want to pay by cash.
     */
    private void confirmCashPayment() {
        if(!shoppingCart.getCartItems().isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder
                        .setMessage("Total is : £" + currencyOut(shoppingCart.getSubtotal()).toString()
                                + ". Pay by cash?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                cashPayment();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .show();
        } else {
            Toast.makeText(this, "Checkout is empty.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Confirm if you want to pay by card.
     */
    private void confirmCardPayment() {
        if(!shoppingCart.getCartItems().isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder
                    .setMessage("Total is : £" + currencyOut(shoppingCart.getSubtotal()).toString()
                            + ". Pay by card?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            cardPayment();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .show();
        } else {
            Toast.makeText(this, "Checkout is empty.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Makes a cash payment, storing the sale in the database.
     */
    public void cashPayment() {
        SaleInfo saleInfo = new SaleInfo();
        saleInfo.setEmpID(currentEmpId);
        saleInfo.setTotalPrice(shoppingCart.getSubtotal());
        System.out.print(shoppingCart.getSubtotal());
        long saleResult = dbHelper.insertSaleData(saleInfo);
        int currentSaleId = (int) saleResult;
        if(saleResult == -1) {
            Toast.makeText(this, "Sale not processed, try again.", Toast.LENGTH_LONG).show();
        } else {
            ArrayList<ShoppingCartItem> tempStock = shoppingCart.getCartItems();
            for(ShoppingCartItem cartItems : tempStock) {
                StockSale stockSale = new StockSale();
                stockSale.setSaleID(currentSaleId);
                stockSale.setStockID(cartItems.getCartItem().getStockId());
                stockSale.setQtySold(cartItems.getQuantity());
                boolean stockSaleResult = dbHelper.insertStockSaleData(stockSale);
                if(!stockSaleResult) {
                    Toast.makeText(this, "Error in stock information, please try again.", Toast.LENGTH_LONG).show();
                    break;
                }
            }
            Intent intent = new Intent(CheckoutActivity.this, PurchasedItemsActivity.class);
            intent.putExtra("ArrayList", shoppingCart);
            intent.putExtra("Payment", "Cash");
            startActivity(intent);
            finish();
        }
    }

    /**
     * Makes a card payment, storing the sale in the database.
     */
    public void cardPayment() {
        Intent intent = new Intent(CheckoutActivity.this, PurchasedItemsActivity.class);
        intent.putExtra("ArrayList", shoppingCart);
        intent.putExtra("Payment", "Card");
        startActivity(intent);
        finish();
    }

    /**
     * Custom Adapter class to handle the ListView for the shopping cart.
     */
    public class CheckoutAdapter extends ArrayAdapter<ShoppingCartItem> {

        private ArrayList<ShoppingCartItem> stockList;

        public CheckoutAdapter(Context context, int resource, ArrayList<ShoppingCartItem> stockList) {
            super(context, resource, stockList);
            this.stockList = new ArrayList<ShoppingCartItem>();
            this.stockList.addAll(stockList);
        }

        private class ViewHolder {
            TextView tvCartItemName;
            TextView tvCartItemQuantity;
            TextView tvCartItemPrice;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;

            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.adapter_cart_item, null);

                holder = new ViewHolder();

                holder.tvCartItemName = (TextView) convertView.findViewById(R.id.tvCartItemName);
                holder.tvCartItemQuantity = (TextView) convertView.findViewById(R.id.tvCartItemQuantity);
                holder.tvCartItemPrice = (TextView) convertView.findViewById(R.id.tvCartItemPrice);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            //Works with the main shoppingCart object but not for the stockList within?
//            ShoppingCartItem shoppingCartItem = stockList.get(position);
            ShoppingCartItem shoppingCartItem = shoppingCart.getCartItems().get(position);

            BigDecimal itemPrice = currencyOut(shoppingCartItem.getTotal());
//            itemPrice = itemPrice.multiply(new BigDecimal(shoppingCartItem.getQuantity()));

            holder.tvCartItemName.setText(shoppingCartItem.getCartItem().getStockName());
            holder.tvCartItemQuantity.setText(Integer.toString(shoppingCartItem.getQuantity()));
            holder.tvCartItemPrice.setText(itemPrice.toString());

            return convertView;
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId()==R.id.lvCheckoutItems) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            menu.setHeaderTitle(shoppingCart.getCartItems().get(info.position - 1).getCartItem().getStockName());
            String[] menuItems = getResources().getStringArray(R.array.checkoutMenu);
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String[] menuItems = getResources().getStringArray(R.array.checkoutMenu);
        String menuItemName = menuItems[menuItemIndex];
        String listItemName = shoppingCart.getCartItems().get(info.position - 1).getCartItem().getStockName();

        switch(menuItemName) {
            case "Remove": {
                //TO-DO remove item
                shoppingCart.getCartItems().remove(info.position - 1);
                mAdapter.notifyDataSetChanged();
                tvTotalPrice.setText(currencyOut(shoppingCart.getSubtotal()).toString());
                Toast.makeText(this, listItemName + " has been removed.", Toast.LENGTH_SHORT).show();
                break;
            }
            case "Increase Qty.": {
                //TO-DO increase Qty
                shoppingCart.getCartItems().get(info.position - 1).increaseQuantity();
                mAdapter.notifyDataSetChanged();
                tvTotalPrice.setText(currencyOut(shoppingCart.getSubtotal()).toString());
//                Toast.makeText(this, menuItemName + " : " + listItemName, Toast.LENGTH_SHORT).show();
                break;
            }
            case "Decrease Qty.": {
                //TO-DO decrease Qty
                shoppingCart.getCartItems().get(info.position - 1).decreaseQuantity();
                if(shoppingCart.getCartItems().get(info.position - 1).getQuantity() == 0) {
                    shoppingCart.getCartItems().remove(info.position - 1);
                    Toast.makeText(this, listItemName + " has been removed.", Toast.LENGTH_SHORT).show();
                }
                mAdapter.notifyDataSetChanged();
                tvTotalPrice.setText(currencyOut(shoppingCart.getSubtotal()).toString());
//                Toast.makeText(this, menuItemName + " : " + listItemName, Toast.LENGTH_SHORT).show();
                break;
            }
        }
        return true;
    }

//    public void increaseQty() {
//
//    }
//
//    public void decreaseQty() {
//
//    }
//
//    public void removeFromCart() {
//
//    }

    /**
     * Converts the currency from pounds into pence.
     * @param currency - pound
     * @return currencyInt - pence
     */
    public int currencyIn(String currency) {
        BigDecimal currencyBD = new BigDecimal(currency);
        currencyBD = currencyBD.multiply(new BigDecimal("100"));
        int currencyInt = currencyBD.intValueExact();
        return currencyInt;
    }

    /**
     * Converts the currency from pence into pounds.
     * @param currency
     * @return
     */
    public BigDecimal currencyOut(int currency) {
        BigDecimal currencyBD = new BigDecimal(currency);
        currencyBD = currencyBD.divide(new BigDecimal("100"));
        currencyBD = currencyBD.setScale(2);
        return currencyBD;
    }
}

